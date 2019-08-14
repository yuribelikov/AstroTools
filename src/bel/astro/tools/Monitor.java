package bel.astro.tools;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;


public class Monitor implements Runnable
{
  private static Logger lgr = Logger.getLogger(Monitor.class.getName());
  private static int error = 0;
  private Properties properties = new Properties();
  private Thread thread = new Thread(this);
  private boolean isAlive = true;

  private int lastGuidingStep = -1;
  private long guidingFailureTime = -1;
  private boolean guidingWasActive = false;


  public static void main(String[] args)
  {
    PropertyConfigurator.configure("log4j.properties");
    new Monitor().thread.start();
  }

  @Override
  public void run()
  {
    try
    {
      lgr.info("");
      lgr.info("");
      lgr.info("Astro Monitor started");

      reloadProperties();
      testRelay();
      scopeTest();
      cameraTest();

      while (isAlive)
      {
        lgr.info("");
        reloadProperties();
        int phd2logDelay = getIntProperty("phd2.log.check.delay", 15);
        lgr.info("phd2logDelay: " + phd2logDelay + " seconds");

        boolean guidingFine = analysePhd2logFile();
        if (!guidingFine)
        {
          lgr.warn("guiding failed and timed out.. starting shutdown sequence..");
          shutdown();
          isAlive = false;
          break;
        }

        long checkOn = System.currentTimeMillis() + 1000 * phd2logDelay;
        while (System.currentTimeMillis() < checkOn)
          Thread.sleep(100);

      }
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }

    lgr.info("Astro Monitor finished.");
    lgr.info("----------------------------------------------------------------------------------------");
  }

  private void reloadProperties()
  {
    try
    {
      FileInputStream fis = new FileInputStream("monitor.properties");
      properties.load(fis);
      fis.close();

    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }
  }

  private void testRelay() throws Exception
  {
    lgr.info("testing relay..");

    execRelay("relay.in.light.on");
    sleepMs(1000);
    execRelay("relay.in.light.off");
    sleepMs(1100);
    if (error != -1)
      throw new Exception("Cannot access relay");

    lgr.info("relay is ok");
  }

  private void execRelay(String property)
  {
    error = 0;
    new Thread(() -> {
      try
      {
        String relayPath = properties.getProperty("relay.path");
        String relayCmd = properties.getProperty(property);
        if (relayCmd == null || relayCmd.length() == 0)
          System.out.println("WARNING: relay command property not found: " + property);
        else
        {
          String command = relayPath + " " + relayCmd;
          lgr.info("execute: " + command);
          Process p = Runtime.getRuntime().exec(command);
          sleepMs(getIntProperty("relay.after.exec.delay", 100));
          Monitor.error = p.getErrorStream().read();
        }
      }
      catch (IOException e)
      {
        lgr.warn(e.getMessage(), e);
        Monitor.error = 1;
      }
    }).start();
  }

  private boolean analysePhd2logFile()    // return true if guiding is in progress or recently failed; false if failed and failure timeout occured
  {
    try
    {
      String phd2logDir = properties.getProperty("phd2.log.dir");
      Path dir = Paths.get(phd2logDir);  // specify your directory
      Optional<Path> lastFilePath = Files.list(dir)    // here we get the stream with full directory listing
        .filter(f -> !Files.isDirectory(f) && f.getFileName().toString().startsWith("PHD2_GuideLog"))  // exclude subdirectories from listing
        .max(Comparator.comparingLong(f -> f.toFile().lastModified()));  // finally get the last file using simple comparator by lastModified field

      if (!lastFilePath.isPresent()) // your folder may be empty
      {
        lgr.info("PHD2 log not found in: " + dir.toFile().getAbsolutePath());
        return false;
      }

      lgr.info("last PHD2 log: " + lastFilePath.get().getFileName());
      BufferedReader reader = Files.newBufferedReader(lastFilePath.get(), StandardCharsets.UTF_8);
      final String guiding_begins = "Guiding Begins";
      String guidingStatus = "undefined";
      String lastLine = "";
      int guidingStep = -1;
      String line;
      while ((line = reader.readLine()) != null)
      {
        if (line.startsWith(guiding_begins) || line.startsWith("Guiding Ends"))
          guidingStatus = line;

        if (line.trim().length() > 0)
        {
          lastLine = line;
          guidingStep = getGuidingStep(line);
        }
      }

      boolean guiding = (!lastLine.contains("DROP") && guidingStatus.startsWith(guiding_begins));
      lgr.info("PHD2 guiding: " + guiding + ", guidingStatus: " + guidingStatus + ", guidingStep: " + guidingStep);
      lgr.info("PHD2 last log line: " + lastLine);
      if (guiding)
        guidingWasActive = true;

      if (!guidingWasActive)
      {
        lgr.info("guiding not started, waiting for guiding starting..");
        return true;
      }

      if (!guiding || guidingStep == lastGuidingStep)   // not guiding or stuck
      {
        lgr.warn(guiding ? "PHD2 log file is not updaing" : "guiding failed");
        if (guidingFailureTime == -1)
          guidingFailureTime = System.currentTimeMillis();

        int phd2guidingFailureTimeout = getIntProperty("phd2.guiding.failure.timeout", 120);
        lgr.info("phd2guidingFailureTimeout: " + phd2guidingFailureTimeout + " seconds");
        lgr.info("PHD2 time to failure timeout: " + (guidingFailureTime + 1000 * phd2guidingFailureTimeout - System.currentTimeMillis()) / 1000 + " seconds");

        return (System.currentTimeMillis() < guidingFailureTime + 1000 * phd2guidingFailureTimeout);
      }
      else
      {
        lastGuidingStep = guidingStep;
        guidingFailureTime = -1;
        return true;
      }

    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }

    return false;
  }

  private int getGuidingStep(String logLine)
  {
    try
    {
      int comma = logLine.indexOf(',');
      return Integer.parseInt(logLine.substring(0, comma));
    }
    catch (Exception ignored)
    {
      return -1;
    }
  }

  private void shutdown() throws Exception
  {
    lgr.info("main mirrow warming up..");
    execRelay("relay.main.mirror.warm.on");

    cameraWarmup();

    scopePark();

    sleepMs(1000);
    lgr.info("checking whether scope parked..");
    sleepMs(1000);
    lgr.info("closing roof..");
    sleepMs(1000);

    lgr.info("shutdown finished.");
  }

  private void scopeTest() throws Exception
  {
    HashMap scopeData = execScript(properties.getProperty("eqmod.scope.data"));
    if (scopeData.size() == 0)
      throw new Exception("Cannot access scope");
  }

  private void scopePark() throws Exception
  {
    lgr.info("parking scope..");
    HashMap scopeData = execScript(properties.getProperty("eqmod.scope.park"));
    if (!scopeData.containsKey("parking"))
      throw new Exception("Cannot park scope");

    long mustParkBy = System.currentTimeMillis() + getIntProperty("scope.park.timeout", 120);
    while (System.currentTimeMillis() < mustParkBy)
    {
      sleepMs(1000);
      scopeData = execScript(properties.getProperty("eqmod.scope.data"));

      if ("true".equals(scopeData.get("atPark")))
        break;
    }

    if (System.currentTimeMillis() < mustParkBy)    // timeout
      throw new Exception("Cannot park scope - timeout occured");

    float azimuth = Float.parseFloat(scopeData.get("azimuth").toString());
    float altitude = Float.parseFloat(scopeData.get("altitude").toString());
    String logPos = azimuth + ", " + altitude+ " (azimuth, altitude)";
    if (Math.abs(azimuth - getIntProperty("scope.park.Azimuth.check", 3)) > getIntProperty("scope.park.check.error", 10) ||
        Math.abs(altitude - getIntProperty("scope.park.Altitude.check", 3)) > getIntProperty("scope.park.check.error", 10))
      throw new Exception("Scope parked in wrong position: " + logPos);

    lgr.info("scope parked successfully on: " + logPos);
  }

  private void cameraTest()
  {
    HashMap cameraData = execScript(properties.getProperty("ascom.camera.data"));
    if (cameraData.size() == 0)
      lgr.warn("Cannot access camera");
  }

  private void cameraWarmup()
  {
    new Thread(() -> {
      try
      {
        lgr.info("warming up camera..");
        // TBD

        sleepMs(5000);
      }
      catch (Exception e)
      {
        lgr.warn(e.getMessage(), e);
      }
    }).start();

    new Thread(() -> {
      try
      {
        sleepMs(getIntProperty("camera.cooling.off.after", 120));
        lgr.info("powering off camera cooler..");
        execRelay("relay.in.light.on");
      }
      catch (Exception e)
      {
        lgr.warn(e.getMessage(), e);
      }
    }).start();

  }

  private HashMap execScript(String scriptName)
  {
    HashMap<String, Object> results = new HashMap<>();
    try
    {
      String command = properties.getProperty("js.script.cmd");
      lgr.info("execute: " + command + " " + scriptName);
      Process p = Runtime.getRuntime().exec(command);
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = br.readLine()) != null)
      {
        lgr.info(line);
        Object[] parsedLine = parseScriptResultLine(line);
        if (parsedLine != null)
          results.put(parsedLine[0].toString(), parsedLine[1]);
      }

      br.close();
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }

    return results;
  }

  private Object[] parseScriptResultLine(String line)
  {
    try
    {
      Object[] result = new String[2];
      if (line.startsWith("##."))
      {
        String[] sa = line.split(":");
        result[0] = sa[0].substring(3).trim();    // key
        result[1] = sa[1].trim();                       // value
        return result;
      }
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }

    return null;
  }

  private void sleepMs(long ms)
  {
    try
    {
      Thread.sleep(ms);
    }
    catch (InterruptedException e)
    {
      lgr.warn(e.getMessage(), e);
    }
  }

  private int getIntProperty(String property, int defaultValue)
  {
    try
    {
      return Integer.parseInt(properties.getProperty(property));
    }
    catch (Exception ignored)
    {
      return defaultValue;
    }
  }

}
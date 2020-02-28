package bel.astro.tools;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;


public class Monitor extends JFrame implements Runnable
{
  private static SimpleDateFormat df1 = new SimpleDateFormat("HH:mm:ss");
  private static Logger lgr = Logger.getLogger(Monitor.class.getName());
  private static int error = 0;
  private Properties properties = new Properties();
  private Thread thread = new Thread(this);
  private boolean isAlive = true;

  private int lastGuidingStep = -1;
  private long guidingFailureTime = -1;
  private boolean guidingWasActive = false;

  private float startAirTemp = Float.MIN_VALUE;
  private int focuserPosition = 0;

  private Rectangle windowRect = new Rectangle(100, 100, 500, 300);
  private Label statusTimeL = new Label();
  private Label powerStatusL = new Label();
  private Label gudingStatusL = new Label();
  private Label gudingStatusInfoL = new Label();
  private Label thermoStabilizationL = new Label();
  private Label thermoStabilizationInfoL = new Label();
  private Label focusCompensationL = new Label();


  public Monitor() throws HeadlessException
  {
    setTitle("Monitor");
    setLayout(null);

    Label stL = new Label("Status updated on:");
    add(stL);
    stL.setBounds(5, 10, 110, 20);
    add(statusTimeL);
    statusTimeL.setBounds(115, 10, 85, 20);

    Label pwrL = new Label("Power:");
    add(pwrL);
    pwrL.setBounds(200, 10, 40, 20);
    add(powerStatusL);
    powerStatusL.setBounds(250, 10, 130, 20);

    Label gsL = new Label("Guiding:");
    add(gsL);
    gsL.setBounds(5, 40, 50, 20);
    add(gudingStatusL);
    gudingStatusL.setBounds(60, 40, 50, 20);
    add(gudingStatusInfoL);
    gudingStatusInfoL.setBounds(110, 40, windowRect.width - 120, 20);

    Label tsL = new Label("Thermostabilization:");
    add(tsL);
    tsL.setBounds(5, 70, 115, 20);
    add(thermoStabilizationL);
    thermoStabilizationL.setBounds(125, 70, 45, 20);
    add(thermoStabilizationInfoL);
    thermoStabilizationInfoL.setBounds(170, 70, windowRect.width - 180, 20);

    add(focusCompensationL);
    focusCompensationL.setBounds(5, 90, windowRect.width - 10, 20);
  }

  public static void main(String[] args)
  {
    PropertyConfigurator.configure("log4j.properties");
    final Monitor monitor = new Monitor();
    if (args.length > 0 && args[0].equals("-s"))
      monitor.shutdown(true);
    else
    {
      monitor.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      monitor.setVisible(true);
      monitor.setBounds(monitor.windowRect);
      monitor.thread.setDaemon(true);
      monitor.thread.start();
    }
  }

  @Override
  public void run()
  {
    try
    {
      reloadProperties();
      lgr.info("");
      lgr.info("");
      lgr.info("Astro Monitor started" + ("true".equals(properties.getProperty("emulation")) ? " in emulation mode" : ""));

//      testRelay();
//      scopeTest();

      while (isAlive)
      {
        lgr.info("");
        reloadProperties();
        int checkDelay = getIntProperty("check.delay", 10);
        lgr.info("checkDelay: " + checkDelay + " seconds");

        statusTimeL.setText(df1.format(new Date()));
        if (!poweredOn())
        {
          lgr.info("");
          lgr.warn("power is off and timed out..");
          shutdown(true);
        }
        else if (!checkGuiding())
        {
          lgr.info("");
          lgr.warn("guiding failed and timed out..");
          shutdown(false);
        }

        temperatureAndFocus();
        sleepS(checkDelay);
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

  private boolean poweredOn()
  {
    final Path powerOffFile = Paths.get(properties.getProperty("power.off.file"));
    if (Files.exists(powerOffFile))
    {
      final long poweredOffAgo = (System.currentTimeMillis() - powerOffFile.toFile().lastModified()) / 1000;
      powerStatusL.setText("OFF for " + poweredOffAgo + " seconds");
      powerStatusL.setForeground(Color.red);
      lgr.warn("no power for " + poweredOffAgo + " seconds");
      return poweredOffAgo < getIntProperty("power.failure.timeout", 30);
    }
    else
    {
      powerStatusL.setText("ON");
      powerStatusL.setForeground(Color.green.darker());
      lgr.warn("power is on");
      return true;
    }
  }

  private boolean checkGuiding()    // return true if guiding is in progress or recently failed; false if failed and failure timeout occured
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
      final String guiding_ends = "Guiding Ends";
      String guidingStatus = "undefined";
      String lastLine = "";
      int guidingStep = -1;
      String line;
      while ((line = reader.readLine()) != null)
      {
        if (line.startsWith(guiding_begins) || line.startsWith(guiding_ends))
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
        gudingStatusL.setText("Waiting");
        gudingStatusL.setForeground(Color.black);
        return true;
      }

      gudingStatusInfoL.setText(lastLine);
      if (!guiding || guidingStep == lastGuidingStep)   // not guiding or stuck
      {
        lgr.warn(guiding ? "PHD2 log file is not updaing" : "guiding failed");
        if (guidingFailureTime == -1)
          guidingFailureTime = System.currentTimeMillis();

        int phd2guidingFailureTimeout = getIntProperty("phd2.guiding.failure.timeout", 5);
        int phd2guidingEndTimeout = getIntProperty("phd2.guiding.end.timeout", 20);
        lgr.info("phd2 guiding timeouts (failures, end), minutes: " + phd2guidingFailureTimeout + ", " + phd2guidingEndTimeout);
        long timeout = guidingStatus.startsWith(guiding_ends) ? phd2guidingEndTimeout : phd2guidingFailureTimeout;    // guiding ends timeout is different (e.g. for meridian flip)
        long secondsToShutdown = (guidingFailureTime + 60 * 1000 * timeout - System.currentTimeMillis()) / 1000;
        lgr.info("time to shutdown: " + secondsToShutdown + " seconds");
        gudingStatusL.setText((guiding ? "Stuck" : "Failed"));
        gudingStatusL.setForeground(Color.red);
        lastLine = lastLine.length() < 50 ? lastLine : lastLine.substring(0, 50);
        long minsToShutdown = secondsToShutdown / 60;
        gudingStatusInfoL.setText(lastLine + "..  " + minsToShutdown + " minutes to shutdown..");
        if (minsToShutdown == 5 || minsToShutdown == 3 || minsToShutdown <= 1)
          this.toFront();

        return secondsToShutdown > 0;
      }
      else
      {
        lastGuidingStep = guidingStep;
        guidingFailureTime = -1;
        gudingStatusL.setText("Active");
        gudingStatusL.setForeground(Color.green.darker());
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

  private void shutdown(boolean fast)
  {
    try
    {
      reloadProperties();

      lgr.info("");
      lgr.warn("starting " + (fast ? "fast " : "") + "shutdown sequence..");
      lgr.info("");
      lgr.info("light on..");
      execRelay("relay.light.out.on");
      sleepS(1);

      lgr.info("");
      lgr.info("starting main mirrow warmup..");
      execRelay("relay.main.mirror.warm.on");
      sleepS(1);

      if ("true".equals(properties.getProperty("camera.cooler.off")))
      {
        lgr.info("");
        lgr.info("powering off camera cooler..");
        execRelay("relay.camera.cooler.off");
        sleepS(1);
      }

      scopePark();
      sleepS(1);

      roofPreClose(fast);
      scopePark();    // this double checks the scope park position - do not remove!
      roofClose();

      lgr.info("shutdown finished.");
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
      lgr.info("");
      lgr.info("shutdown aborted.");
    }

    sleepS(1);
    lgr.info("light off..");
    execRelay("relay.light.out.off");

    sleepS(2);
    isAlive = false;
  }

  private void scopeTest() throws Exception
  {
    lgr.info("");
    lgr.info("testing scope..");
    HashMap scopeData = execScript(properties.getProperty("eqmod.scope.data"));
    if (scopeData.size() == 0)
      throw new Exception("Cannot access scope");
  }

  private void scopePark() throws Exception
  {
    lgr.info("");
    lgr.info("parking scope..");
    HashMap scopeData = execScript(properties.getProperty("eqmod.scope.park"));
    if (!scopeData.containsKey("parking"))
      throw new Exception("Cannot park scope");

    long mustParkBy = System.currentTimeMillis() + 1000 * getIntProperty("scope.park.timeout", 120);
    while (System.currentTimeMillis() < mustParkBy)
    {
      sleepS(5);
      scopeData = execScript(properties.getProperty("eqmod.scope.data"));

      if ("true".equals(scopeData.get("atPark")))
        break;
    }

    if (System.currentTimeMillis() >= mustParkBy)    // timeout
      throw new Exception("Cannot park scope - timeout occured");

    lgr.info("");
    lgr.info("scope parked.");
    lgr.info("");
    lgr.info("verifying scope park position..");
    float azimuth = Float.parseFloat(scopeData.get("azimuth").toString());
    float altitude = Float.parseFloat(scopeData.get("altitude").toString());
    String logPos = azimuth + ", " + altitude + " (azimuth, altitude)";
    final int maxError = getIntProperty("scope.park.check.error", 10);
    final float azError = Math.abs(azimuth - getIntProperty("scope.park.azimuth.check", 3));
    final float altError = Math.abs(altitude - getIntProperty("scope.park.altitude.check", 3));
    lgr.info("azError: " + azError + ", altError: " + altError);
    if (azError > maxError || altError > maxError)
      throw new Exception("Scope parked in wrong position: " + logPos);

    lgr.info("scope parked successfully on: " + logPos);
  }

  private void roofPreClose(boolean fast)
  {
    lgr.info("");
    lgr.info("pre-closing roof..");
    execRelay("relay.roof.pre.close.prepare");
    sleepS(1);
    execRelay("relay.roof.pre.close.start");
    sleepS(getFloatProperty("roof.pre.close.cmd.duration", 3.0f));
    execRelay("relay.roof.pre.close.stop");
    lgr.info("roof pre-closed.");

    float wait = fast ? 5 : getFloatProperty("roof.pre.close.pause", 120);
    lgr.info("waiting for " + wait + "seconds");
    sleepS(wait);
  }

  private void roofClose()
  {
    lgr.info("");
    lgr.info("closing roof..");
    String[] sa = properties.getProperty("roof.close.sequence").split(",");
    double[] closeSequence = new double[sa.length];
    for (int i = 0; i < sa.length; i++)
      closeSequence[i] = Double.parseDouble(sa[i].trim());

    execRelay("relay.roof.close.start");
    for (int i = 0; i < closeSequence.length; i++)
    {
      sleepS(closeSequence[i]);
      if (i % 2 == 0)       // roof.close.sequence = 0.5, 10, 0.5, 10, 0.5, 10
        execRelay("relay.roof.move.off");
      else
        execRelay("relay.roof.move.on");
    }

    execRelay("relay.roof.all.off");
    lgr.info("roof closed.");
  }

  private HashMap execScript(String scriptName)
  {
    HashMap<String, String> results = new HashMap<>();
    try
    {
      String emulationPrefix = " ";
      if ("true".equals(properties.getProperty("emulation")))
      {
        lgr.info("emulation mode - using simulator scripts..");
        emulationPrefix = " sim_";
      }

      String command = properties.getProperty("js.script.cmd") + emulationPrefix + scriptName;
      lgr.info("execute: " + command);
      Process p = Runtime.getRuntime().exec(command);
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = br.readLine()) != null)
      {
        if (line.contains("Microsoft") || line.trim().length() == 0)
          continue;

        lgr.info(line);
        String[] parsedLine = parseScriptResultLine(line);
        if (parsedLine != null)
          results.put(parsedLine[0], parsedLine[1]);
      }

      br.close();
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }

    return results;
  }

  private String[] parseScriptResultLine(String line)
  {
    try
    {
      String[] result = new String[2];
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

  private void testRelay() throws Exception
  {
    lgr.info("");
    lgr.info("testing relay..");
    execRelay("relay.test");
    sleepS(1);
    if (error != -1)
      throw new Exception("Cannot access relay");

    lgr.info("relay is ok");
  }

  private void execRelay(String property)
  {
    error = 0;
    new Thread(() ->
    {
      try
      {
        String relayPath = properties.getProperty("relay.path");
        String relayCmd = properties.getProperty(property);
        if (relayCmd == null || relayCmd.length() == 0)
          lgr.warn("WARNING: relay command property not found: " + property);
        else
        {
          String command = relayPath + " " + relayCmd;
          lgr.info("execute: " + command);
          if ("true".equals(properties.getProperty("emulation")))
          {
            lgr.info("emulation mode - no real action..");
            error = -1;
            return;
          }

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

  private void temperatureAndFocus()
  {
    lgr.info("");
    try
    {
      String tempFile = properties.getProperty("temp.file");
      lgr.info("getting current temperature from: " + tempFile);
      HashMap<String, Float> sensorsData = new HashMap<>();
      BufferedReader reader = Files.newBufferedReader(Paths.get(tempFile), StandardCharsets.UTF_8);
      String line;
      if ((line = reader.readLine()) != null)
      {
        String[] sensors = line.split(" ");
        for (String sensor : sensors)
        {
          if (!sensor.contains("="))
            continue;
          String[] sa = sensor.split("=");
          sensorsData.put(sa[0], Float.parseFloat(sa[1].replace(",", ".")));
        }
      }
      else
        lgr.warn("no data in temp file..");

      String airTempSensor = properties.getProperty("temp.air.sensor");
      float currAirTemp = sensorsData.get(airTempSensor);
      if (currAirTemp == Float.MIN_VALUE)
      {
        lgr.warn("cannot get " + airTempSensor + " value from line: " + line);
        return;
      }

      lgr.info("current temp: " + airTempSensor + " = " + currAirTemp);
      if (startAirTemp == Float.MIN_VALUE)
      {
        lgr.info("setting start temperature to: " + currAirTemp);
        startAirTemp = currAirTemp;
      }
      else
      {
        float diff = currAirTemp - startAirTemp;
        float compensation = diff * getIntProperty("focuser.compensation", -100);
        int focuserStepSize = getIntProperty("focuser.step.size", 10);
        lgr.info("diff with " + startAirTemp + " is " + diff + ", compensation: " + compensation + ", focuserPosition : " + focuserPosition);
        if (Math.abs(compensation - focuserPosition) >= focuserStepSize)
        {
          String dir = compensation > focuserPosition ? "up" : "down";
          execScript(properties.getProperty("focuser." + dir + ".script"));
          focuserPosition += compensation > focuserPosition ? focuserStepSize : -focuserStepSize;
          lgr.info("focuserPosition: " + focuserPosition);
        }

        focusCompensationL.setText("Air: " + currAirTemp + "   (started: " + startAirTemp + ", diff: " + diff + ")    focuser compensation: " + focuserPosition);
      }

      String mainMirrorTempSensor = properties.getProperty("temp.main.mirror.sensor");
      float mirrorTemp = sensorsData.get(mainMirrorTempSensor);
      int thermostabilization = 100 - Math.round(10 * Math.abs(mirrorTemp - currAirTemp));
      thermoStabilizationL.setText(thermostabilization + "%");
      thermoStabilizationInfoL.setText("main mirror: " + mirrorTemp + ", diff: " + (mirrorTemp - currAirTemp));
      if (currAirTemp - mirrorTemp > 0.5)
      {
        int b = Math.round(100 * (currAirTemp - mirrorTemp));
        thermoStabilizationL.setForeground(new Color(0, 0, b > 255 ? 255 : b));
      }
      else
      {
        int r = Math.round(2.55f * (100 - thermostabilization));
        int g = Math.round(1.76f * Math.abs(thermostabilization));
        thermoStabilizationL.setForeground(new Color(r < 255 ? r : 255, g, 0));
      }
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }
  }

  private void sleepMs(long ms)
  {
    try
    {
      long sleepBy = System.currentTimeMillis() + ms;
      while (isAlive && System.currentTimeMillis() < sleepBy)
        Thread.sleep(5);
    }
    catch (InterruptedException e)
    {
      lgr.warn(e.getMessage(), e);
    }
  }

  private void sleepS(double seconds)
  {
    sleepMs((int) (1000 * seconds));
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

  private float getFloatProperty(String property, float defaultValue)
  {
    try
    {
      return Float.parseFloat(properties.getProperty(property));
    }
    catch (Exception ignored)
    {
      return defaultValue;
    }
  }

}
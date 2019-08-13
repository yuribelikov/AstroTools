package bel.astro.tools;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;


public class Monitor implements Runnable
{
  private static Logger lgr = Logger.getLogger(Monitor.class.getName());
  private static SimpleDateFormat df1 = new SimpleDateFormat("HH:mm:ss");
  private static int error = 0;
  private Properties properties = new Properties();
  private Thread thread = new Thread(this);
  private boolean isAlive = true;

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

      while (isAlive)
      {
        reloadProperties();

        long checkOn = System.currentTimeMillis() + getIntProperty("phd2.log.check.delay", 30000);
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

  private void execRelay(String property) throws Exception
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
          String command = "cmd /c start " + relayPath + " " + relayCmd;
          lgr.info("execute: " + command);
          Process p = Runtime.getRuntime().exec(command);
          sleepMs(1000);
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

  private void sleepMs(long ms)
  {
    try
    {
      Thread.sleep(ms);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
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
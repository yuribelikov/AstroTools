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
  private Properties properties = new Properties();

  private Thread thread = new Thread(this);
  private boolean isAlive = true;

  private static int error = 0;


  private Monitor()
  {
    try
    {
      FileInputStream fis = new FileInputStream("monitor.properties");
      properties.load(fis);
      fis.close();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void main(String[] args)
  {
    PropertyConfigurator.configure("log4j.properties");

    Monitor monitor = new Monitor();
    monitor.thread.start();

//    monitor.isAlive = false;
  }

  @Override
  public void run()
  {
    try
    {
      lgr.info("");
      lgr.info("");
      lgr.info("Astro Monitor started");

      execRelay("relay.test");

      while (isAlive)
      {
        int phd2LogCheckDelay = 30000;
        try
        {
          phd2LogCheckDelay = Integer.parseInt(properties.getProperty("phd2.log.check.delay"));
        }
        catch (Exception ignored)
        {
        }

        long now = System.currentTimeMillis();

        sleepMs(1000);
        execRelay("relay.light.on");
        sleepMs(1000);
        execRelay("relay.light.off");


        Thread.sleep(100);
      }
    }
    catch (Exception e)
    {
      lgr.warn(e.getMessage(), e);
    }

    lgr.info("Astro Monitor finished.");
  }

  private void execRelay(String property) throws Exception
  {
    error = 0;
    new Thread()
    {
      public void run()
      {

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
        }
      }
    }.start();

    sleepMs(1100);
    if (error != -1)
      throw new Exception("Cannot access relay");
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

}
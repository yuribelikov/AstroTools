package bel.astro.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;


public class Snapper extends JFrame implements ActionListener, Runnable
{
  private Point windowPos;
  private Point snapPos;
  private Point taskBarPos;
  private Point hidePos;

  private int exposure;
  private int delay;

  private Robot bot;
  private boolean eqModShown = false;
  private boolean exposing = false;

  private JTextField exposureLenTF;
  private JButton shotBtn;
  private JButton startBtn;
  private JButton stopBtn;
  private Label infoL;

  private Thread thread = new Thread(this);
  private Label currTimeL = new Label();
  private JTextField shutdownTF;
  private Label totalExposureL = new Label();
  private Label minutesToParkL = new Label();
  private JButton copyBtn;


  private Snapper() throws HeadlessException
  {
    Properties p = new Properties();
    try
    {
      FileInputStream fis = new FileInputStream("snapper.properties");
      p.load(fis);
      fis.close();

      windowPos = parsePoint(p.getProperty("window.position"));
      snapPos = parsePoint(p.getProperty("snap.position"));
      taskBarPos = parsePoint(p.getProperty("task.bar.position"));
      hidePos = parsePoint(p.getProperty("hide.position"));
      delay = Integer.parseInt(p.getProperty("delay"));

      setTitle("Snapper");
      setLayout(null);
      Label exposureL = new Label("Exposure:");
      add(exposureL);
      exposureL.setBounds(5, 10, 60, 20);
      add(exposureLenTF = new JTextField(p.getProperty("exposure")));
      exposureLenTF.setBounds(70, 10, 35, 20);

      add(shotBtn = new JButton("Shot"));
      shotBtn.addActionListener(this);
      shotBtn.setBounds(110, 10, 60, 20);
      add(startBtn = new JButton("Start"));
      startBtn.addActionListener(this);
      startBtn.setBounds(5, 40, 80, 20);
      add(stopBtn = new JButton("Stop"));
      stopBtn.addActionListener(this);
      stopBtn.setBounds(90, 40, 80, 20);
      stopBtn.setEnabled(false);

      add(infoL = new Label("Not started"));
      infoL.setBounds(5, 70, 180, 20);

      bot = new Robot();

      add(currTimeL);
      currTimeL.setForeground(Color.BLUE);
      currTimeL.setBounds(190, 10, 50, 20);
      Label shutdownL = new Label("Shutdown on:");
      add(shutdownL);
      shutdownL.setBounds(250, 10, 80, 20);
      add(shutdownTF = new JTextField(p.getProperty("shutdown")));
      shutdownTF.setBounds(330, 10, 40, 20);

      Label totalExpL = new Label("Total exposure:");
      add(totalExpL);
      totalExpL.setBounds(190, 40, 90, 20);
      add(totalExposureL);
      totalExposureL.setBounds(280, 40, 40, 20);

      Label minsToParkL = new Label("Minutes to park:");
      add(minsToParkL);
      minsToParkL.setBounds(190, 60, 90, 20);
      add(minutesToParkL);
      minutesToParkL.setBounds(280, 60, 40, 20);
      add(copyBtn = new JButton("cp"));
      copyBtn.addActionListener(this);
      copyBtn.setBounds(320, 60, 60, 20);

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void main(String[] args)
  {
    Snapper snapper = new Snapper();
    snapper.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    snapper.setVisible(true);
    snapper.setBounds(snapper.windowPos.x, snapper.windowPos.y, 400, 130);
    snapper.thread.setDaemon(true);
    snapper.thread.start();
  }

  private void showEqMod()
  {
    toFront();
    bot.mouseMove(taskBarPos.x, taskBarPos.y);
    bot.mousePress(InputEvent.BUTTON1_MASK);
    bot.mouseRelease(InputEvent.BUTTON1_MASK);
    eqModShown = true;
    moveBack();
  }

  private void hideEqMod()
  {
    bot.mouseMove(hidePos.x, hidePos.y);
    bot.mousePress(InputEvent.BUTTON1_MASK);
    bot.mouseRelease(InputEvent.BUTTON1_MASK);
    eqModShown = false;
    moveBack();
  }

  private void snap()
  {
    bot.mouseMove(snapPos.x, snapPos.y);
    bot.mousePress(InputEvent.BUTTON1_MASK);
    bot.mouseRelease(InputEvent.BUTTON1_MASK);
    moveBack();
  }

  private void moveBack()
  {
    bot.mouseMove(getLocation().x + getSize().width - 20, getLocation().y + getSize().height - 20);
  }

  private void shot()
  {
    showEqMod();
    sleepMs(1000);
    snap();
    sleepMs(1000 * exposure);
    snap();
    sleepMs(1000);
    hideEqMod();
    moveBack();
  }

  private void expose()
  {
    new Thread()
    {
      public void run()
      {
        startBtn.setEnabled(false);
        shotBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        if (exposure < 10)
          exposure = 10;

        exposing = true;
        boolean open = false;
        long started = 0;
        while (exposing)
        {
          if (!open)
          {
            if (!eqModShown)
            {
              showEqMod();
              sleepMs(1000);
            }
            snap();
            started = System.currentTimeMillis();
            open = true;
          }
          else
          {
            long t = (System.currentTimeMillis() - started) / 1000;
            String text = t + " sec (" + (exposure - t) + " left)";
            if (t < exposure)
            {
              if (t >= 3 && t <= 5 && eqModShown)    // hide after 5 sec
                hideEqMod();
              if (exposure - t <= 3 && !eqModShown)    // show before for 5 sec before completion
                showEqMod();
              infoL.setText("Exposing for:  " + text);
            }
            else
            {
              infoL.setText("Completed:  " + text);
              snap();
              open = false;
              sleepMs(delay * 1000);
            }
          }
          sleepMs(100);
        }

        if (open)
        {
          showEqMod();
          sleepMs(1000);
          snap();
          infoL.setText("Stopped on:  " + (System.currentTimeMillis() - started) / 1000 + " sec");
          sleepMs(2000);
        }

        startBtn.setEnabled(true);
        shotBtn.setEnabled(true);
        stopBtn.setEnabled(false);
      }
    }.start();
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
//    System.out.println(e);
    exposure = Integer.parseInt(exposureLenTF.getText());
    if (e.getSource() == shotBtn)
      shot();
    else if (e.getSource() == startBtn)
      expose();
    else if (e.getSource() == stopBtn)
      exposing = false;
    else if (e.getSource() == copyBtn)
    {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      StringSelection ss = new StringSelection(minutesToParkL.getText());
      clipboard.setContents(ss, ss);
    }

  }

  private Point parsePoint(String pointStr) throws Exception
  {
    String[] sa = pointStr.split(",");
    return new Point(Integer.parseInt(sa[0].trim()), Integer.parseInt(sa[1].trim()));
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

  @Override
  public void run()
  {
    try
    {
      SimpleDateFormat df1 = new SimpleDateFormat("HH:mm:ss");
      while (thread.isAlive())
      {
        currTimeL.setText(df1.format(new Date()));
        long secondsDiff = (parseTime(shutdownTF.getText()) - System.currentTimeMillis()) / 1000;
        long hours = secondsDiff / 3600;
        long minutes = (secondsDiff - 3600 * hours) / 60;
        totalExposureL.setText(hours + (minutes < 10 ? ":0" : ":") + minutes);
        minutesToParkL.setText("" + secondsDiff / 60);

        Thread.sleep(100);
      }
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  private long parseTime(String time)
  {
    try
    {
      int idx = time.indexOf(':');
      int hours = Integer.parseInt(time.substring(0, idx));
      int minutes = Integer.parseInt(time.substring(idx + 1));
      Calendar c = Calendar.getInstance();
      c.add(Calendar.DATE, 1);
      c.set(Calendar.HOUR_OF_DAY, hours);
      c.set(Calendar.MINUTE, minutes);
      return c.getTimeInMillis();
    }
    catch (Exception ignored)
    {
    }

    return 0;
  }
}
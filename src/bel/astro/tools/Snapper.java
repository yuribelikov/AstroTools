package bel.astro.tools;

import javax.swing.*;
import javax.swing.plaf.metal.MetalBorders;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;


public class Snapper extends JFrame implements ActionListener, Runnable
{
  private static SimpleDateFormat df1 = new SimpleDateFormat("HH:mm:ss");
  private Properties properties = new Properties();

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
  private JButton recBtn;
  private boolean isRecording = false;
  private Point mousePoint = new Point();
  private long mouseMoveTime = 0;
  private JButton execBtn;
  private boolean isExecuting = false;
  private JTextArea actionsTA;
  private ArrayList<MouseAction> mouseActions = new ArrayList<>();
  private Label currentActionL = new Label();


  private Snapper() throws HeadlessException
  {
    try
    {
      FileInputStream fis = new FileInputStream("snapper.properties");
      properties.load(fis);
      fis.close();

      windowPos = parsePoint(properties.getProperty("window.position"));
      snapPos = parsePoint(properties.getProperty("snap.position"));
      taskBarPos = parsePoint(properties.getProperty("task.bar.position"));
      hidePos = parsePoint(properties.getProperty("hide.position"));
      delay = Integer.parseInt(properties.getProperty("delay"));

      setTitle("Snapper");
      setLayout(null);
      Label exposureL = new Label("Exposure:");
      add(exposureL);
      exposureL.setBounds(5, 10, 60, 20);
      add(exposureLenTF = new JTextField(properties.getProperty("exposure")));
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
      add(shutdownTF = new JTextField(properties.getProperty("shutdown")));
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

      add(recBtn = new JButton("Rec"));
      recBtn.addActionListener(this);
      recBtn.setBounds(190, 90, 60, 20);
      add(execBtn = new JButton("Execute"));
      execBtn.addActionListener(this);
      execBtn.setBounds(300, 90, 80, 20);
      add(actionsTA = new JTextArea());
      actionsTA.setBounds(190, 115, 180, 120);
      actionsTA.setBorder(new MetalBorders.TextFieldBorder());
      add(currentActionL);
      currentActionL.setBounds(190, 235, 115, 20);

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
    snapper.setBounds(snapper.windowPos.x, snapper.windowPos.y, 400, 300);
    snapper.setAlwaysOnTop(true);
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
    else if (e.getSource() == recBtn)
    {
      isRecording = !isRecording;
      recBtn.setForeground(isRecording ? Color.red : Color.black);
      updateActionsTA();
      execBtn.setEnabled(!isRecording);
    }
    else if (e.getSource() == execBtn)
      execute();

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
      while (thread.isAlive())
      {
        int recDelay = 5000;
        try
        {
          recDelay = Integer.parseInt(properties.getProperty("rec.delay"));
        }
        catch (Exception ignored)
        {
        }

        long now = System.currentTimeMillis();
        currTimeL.setText(df1.format(new Date()));
        long secondsDiff = (parseTime(shutdownTF.getText()) - now) / 1000;
        while (secondsDiff < -5)
          secondsDiff += 60 * 60 * 24;
        long hours = secondsDiff / 3600;
        long minutes = (secondsDiff - 3600 * hours) / 60;
        totalExposureL.setText(hours + (minutes < 10 ? ":0" : ":") + minutes);
        minutesToParkL.setText("" + secondsDiff / 60);

        if (secondsDiff <= 0 && !isExecuting)
          execute();

        if (isRecording)
        {
          Point p = MouseInfo.getPointerInfo().getLocation();
          if (!p.equals(mousePoint))
          {
            mousePoint = p;
            mouseMoveTime = now;
          }
          else if (now - mouseMoveTime > 1000 * recDelay)
          {
            mouseActions.add(new MouseAction(mouseActions.size(), mousePoint));
            mouseMoveTime = now;
            Toolkit.getDefaultToolkit().beep();
          }
          updateActionsTA();
        }
        else if (!isExecuting)
          parseActionsFromTA();

        Thread.sleep(100);
      }
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  private void execute()
  {
    new Thread()
    {
      public void run()
      {
        isExecuting = true;
        recBtn.setEnabled(false);
        execBtn.setForeground(Color.red);
        sleepMs(1000);
        try
        {
          Runtime.getRuntime().exec(properties.getProperty("rodos3.exe"));
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        for (MouseAction ms : mouseActions)
        {
          currentActionL.setText(ms.toString());
          sleepMs(1000 * ms.delay);
          bot.mouseMove(ms.p.x, ms.p.y);
          sleepMs(800);
          bot.mousePress(InputEvent.BUTTON1_MASK);
          bot.mouseRelease(InputEvent.BUTTON1_MASK);
          Toolkit.getDefaultToolkit().beep();
        }
        sleepMs(1000);
        recBtn.setEnabled(true);
        execBtn.setForeground(Color.black);
        currentActionL.setText("finished on " + df1.format(new Date()));
        isExecuting = false;
      }
    }.start();
  }

  private long parseTime(String time)
  {
    try
    {
      int idx = time.indexOf(':');
      int hours = Integer.parseInt(time.substring(0, idx));
      int minutes = Integer.parseInt(time.substring(idx + 1));
      Calendar c = Calendar.getInstance();
      c.set(Calendar.HOUR_OF_DAY, hours);
      c.set(Calendar.MINUTE, minutes);
      c.set(Calendar.SECOND, 0);
      return c.getTimeInMillis();
    }
    catch (Exception ignored)
    {
    }

    return 0;
  }

  private void updateActionsTA()
  {
    String s = "";
    for (MouseAction ma : mouseActions)
      s += ma + "\n";

    if (isRecording)
      s += ">> " + mousePoint.x + ", " + mousePoint.y;

    actionsTA.setText(s);
  }

  private void parseActionsFromTA()
  {
    mouseActions.clear();
    String[] sa = actionsTA.getText().split("\n");
    for (String s : sa)
    {
      MouseAction ma = new MouseAction(s);
      if (ma.p != null)
        mouseActions.add(ma);
    }

  }

  private class MouseAction
  {
    int delay;
    Point p;

    MouseAction(int delay, Point p)
    {
      this.delay = delay;
      this.p = p;
    }

    MouseAction(String s)
    {
      try
      {
        s = s.trim();
        int i = s.indexOf(':');
        delay = Integer.parseInt(s.substring(0, i));
        int i2 = s.indexOf(',');
        p = new Point(Integer.parseInt(s.substring(i + 1, i2).trim()), Integer.parseInt(s.substring(i2 + 1).trim()));
      }
      catch (Exception ignored)
      {
      }
    }

    @Override
    public String toString()
    {
      return delay + ": " + p.x + ", " + p.y;
    }
  }
}
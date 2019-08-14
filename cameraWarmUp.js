var camera = new ActiveXObject("ASCOM.Simulator.Camera");    // Change for your driver's ID
//var camera = new ActiveXObject("EQMOD.Telecamera");    // Change for your driver's ID
camera.Connected = true;

WScript.StdOut.WriteLine("##.name: " + camera.Name);
WScript.StdOut.WriteLine("##.coolerOn: " + camera.CoolerOn);
WScript.StdOut.WriteLine("##.CCDTemperature: " + camera.CCDTemperature);
WScript.StdOut.WriteLine("##.setCCDTemperature: " + camera.SetCCDTemperature);
var targetTemp = camera.CCDTemperature + 2;
if (targetTemp > camera.SetCCDTemperature)
  camera.SetCCDTemperature = targetTemp;
else
{
  WScript.StdOut.WriteLine("cannot perform warmup algorithm because targetTemp < camera.SetCCDTemperature");
  WScript.StdOut.WriteLine("increasing camera.SetCCDTemperature by 1");
  camera.SetCCDTemperature = camera.SetCCDTemperature + 1;
}

WScript.StdOut.WriteLine("##.setCCDTemperature: " + camera.SetCCDTemperature);

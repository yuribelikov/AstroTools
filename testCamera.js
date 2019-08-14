var camera = new ActiveXObject("ASCOM.Simulator.Camera");    // Change for your driver's ID
//var camera = new ActiveXObject("EQMOD.Telecamera");    // Change for your driver's ID
camera.Connected = true;

WScript.StdOut.WriteLine("Name: " + camera.Name);
WScript.StdOut.WriteLine("SensorName: " + camera.SensorName);
WScript.StdOut.WriteLine("SensorType: " + camera.SensorType);
WScript.StdOut.WriteLine("DriverInfo: " + camera.DriverInfo);
WScript.StdOut.WriteLine("CameraState: " + camera.CameraState);
WScript.StdOut.WriteLine("CameraXSize: " + camera.CameraXSize);
WScript.StdOut.WriteLine("CameraYSize: " + camera.CameraYSize);
WScript.StdOut.WriteLine("PixelSizeX: " + camera.PixelSizeX);
WScript.StdOut.WriteLine("PixelSizeY: " + camera.PixelSizeY);
WScript.StdOut.WriteLine("CanAbortExposure: " + camera.CanAbortExposure);
WScript.StdOut.WriteLine("CanGetCoolerPower: " + camera.CanGetCoolerPower);
WScript.StdOut.WriteLine("CanSetCCDTemperature: " + camera.CanSetCCDTemperature);
WScript.StdOut.WriteLine("CoolerOn: " + camera.CoolerOn);
WScript.StdOut.WriteLine("CCDTemperature: " + camera.CCDTemperature);
WScript.StdOut.WriteLine("SetCCDTemperature: " + camera.SetCCDTemperature);
WScript.StdOut.WriteLine("Gain: " + camera.Gain);
WScript.StdOut.WriteLine("MaxBinX: " + camera.MaxBinX);


camera.SetCCDTemperature(3);
WScript.StdOut.WriteLine("SetCCDTemperature: " + camera.SetCCDTemperature);



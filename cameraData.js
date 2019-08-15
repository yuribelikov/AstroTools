var camera = new ActiveXObject("ASCOM.Simulator.Camera");    // Change for your driver's ID
//var camera = new ActiveXObject("ASCOM.QHY8L.Camera");    // Change for your driver's ID
//var camera = new ActiveXObject("ASCOM.ASCOM_QHY5LII.Camera");    // Change for your driver's ID
//camera.Connected = true;

//WScript.StdOut.WriteLine("##.name: " + camera.Name);
//WScript.StdOut.WriteLine("##.sensorName: " + camera.SensorName);
//WScript.StdOut.WriteLine("##.sensorType: " + camera.SensorType);
//WScript.StdOut.WriteLine("##.driverInfo: " + camera.DriverInfo);
WScript.StdOut.WriteLine("##.cameraState: " + camera.CameraState);
WScript.StdOut.WriteLine("##.cameraXSize: " + camera.CameraXSize);
WScript.StdOut.WriteLine("##.cameraYSize: " + camera.CameraYSize);
WScript.StdOut.WriteLine("##.pixelSizeX: " + camera.PixelSizeX);
WScript.StdOut.WriteLine("##.pixelSizeY: " + camera.PixelSizeY);
//WScript.StdOut.WriteLine("##.gain: " + camera.Gain);
//WScript.StdOut.WriteLine("##.maxBinX: " + camera.MaxBinX);
WScript.StdOut.WriteLine("##.canAbortExposure: " + camera.CanAbortExposure);
WScript.StdOut.WriteLine("##.canGetCoolerPower: " + camera.CanGetCoolerPower);
WScript.StdOut.WriteLine("##.canSetCCDTemperature: " + camera.CanSetCCDTemperature);
WScript.StdOut.WriteLine("##.coolerOn: " + camera.CoolerOn);
WScript.StdOut.WriteLine("##.coolerPower: " + camera.CoolerPower);
//WScript.StdOut.WriteLine("##.CCDTemperature: " + camera.CCDTemperature);
WScript.StdOut.WriteLine("##.setCCDTemperature: " + camera.SetCCDTemperature);

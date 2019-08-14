var scope = new ActiveXObject("EQMOD_sim.Telescope");    // Change for your driver's ID
//var scope = new ActiveXObject("ASCOM.Simulator.Telescope"); 
//var scope = new ActiveXObject("EQMOD.Telescope");    // Change for your driver's ID
scope.Connected = true;

WScript.StdOut.WriteLine("Name: " + scope.Name);
WScript.StdOut.WriteLine("Parked: " + scope.AtPark);
WScript.StdOut.WriteLine("Slewing: " + scope.Slewing);
WScript.StdOut.WriteLine("Altitude: " + scope.Altitude);
WScript.StdOut.WriteLine("Azimuth: " + scope.Azimuth);
WScript.StdOut.WriteLine("Declination: " + scope.Declination);
WScript.StdOut.WriteLine("RightAscension: " + scope.RightAscension);

scope.Unpark();
scope.SlewToCoordinates(21, 5);
WScript.StdOut.WriteLine("Slewing: " + scope.Slewing);
//scope.SlewToAltAz(200, 30);
scope.tracking = true;

WScript.StdOut.WriteLine("Altitude: " + scope.Altitude);
WScript.StdOut.WriteLine("Azimuth: " + scope.Azimuth);
WScript.StdOut.WriteLine("Declination: " + scope.Declination);
WScript.StdOut.WriteLine("RightAscension: " + scope.RightAscension);

WScript.StdOut.WriteLine("Parking..............");

scope.Park();
WScript.StdOut.WriteLine("Parked: " + scope.AtPark);
WScript.StdOut.WriteLine("Slewing: " + scope.Slewing);
WScript.StdOut.WriteLine("Altitude: " + scope.Altitude);
WScript.StdOut.WriteLine("Azimuth: " + scope.Azimuth);
WScript.StdOut.WriteLine("Declination: " + scope.Declination);
WScript.StdOut.WriteLine("RightAscension: " + scope.RightAscension);

//scope.SetPark();
//scope.Park();


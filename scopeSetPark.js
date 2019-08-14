var scope = new ActiveXObject("EQMOD_sim.Telescope");    // Change for your driver's ID
//var scope = new ActiveXObject("EQMOD.Telescope");    // Change for your driver's ID
scope.Connected = true;

WScript.StdOut.WriteLine("name: " + scope.Name);
WScript.StdOut.WriteLine("parked: " + scope.AtPark);

if (scope.AtPark)
{
  // WScript.StdOut.WriteLine("unparking scope..");   - not sure that unparking scope is required
  // scope.Unpark();
  // WScript.StdOut.WriteLine("parked: " + scope.AtPark);
  WScript.StdOut.WriteLine("set current scope position as the scope park position..");
  scope.SetPark();
  // WScript.StdOut.WriteLine("parking scope..");
  // scope.Park();
  // WScript.StdOut.WriteLine("parked: " + scope.AtPark);
  WScript.StdOut.WriteLine("done.");
}
else
  WScript.StdOut.WriteLine("scope is not parked - exiting..");

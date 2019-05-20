package devhub.team.livetracking;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        firebaseDatabase = FirebaseDatabase.getInstance();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //changeMarker();
    }

    public void getData(){
        DatabaseReference databaseReference = firebaseDatabase.getReference().child("position");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mMap.clear();
                long childrenCount = dataSnapshot.getChildrenCount();
                LatLng origin=null;

                if(childrenCount<1)
                {
                    Log.e("No","Data");

                }else{
                    if(!dataSnapshot.child("1").hasChild("lat")||!dataSnapshot.child("1").hasChild("long"))
                    {
                        Toast.makeText(MapsActivity.this,"No position is set",Toast.LENGTH_SHORT).show();
                    }else {
                        origin = new LatLng(dataSnapshot.child("1").child("lat").getValue(Double.class),
                                dataSnapshot.child("1").child("long").getValue(Double.class));
                        mMap.addMarker(new MarkerOptions()
                                .position(origin)
                                .title("Origin")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 10));
                        if(!dataSnapshot.child(String.valueOf(childrenCount)).hasChild("lat")||!dataSnapshot.child(String.valueOf(childrenCount)).hasChild("long")){



                        }else{

                            LatLng destination = new LatLng(dataSnapshot.child(String.valueOf(childrenCount)).child("lat").getValue(Double.class),
                                    dataSnapshot.child(String.valueOf(childrenCount)).child("long").getValue(Double.class));
                            mMap.addMarker(new MarkerOptions()
                                    .position(destination)
                                    .title("Destination")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            Log.e("Origin",String.valueOf(origin));
                            Log.e("Destination",String.valueOf(destination));
                            ArrayList<LatLng> waypoints  = new ArrayList<LatLng>();
                            for(long i=2;i<childrenCount;i++)
                            {
                                //Log.e("waypoint "+String.valueOf(i),String.valueOf(waypoint));
                                if(!dataSnapshot.child(String.valueOf(i)).hasChild("lat")||!dataSnapshot.child(String.valueOf(i)).hasChild("long"))
                                {
                                    continue;
                                }else{
                                    LatLng waypoint = new LatLng(dataSnapshot.child(String.valueOf(i)).child("lat").getValue(Double.class),
                                            dataSnapshot.child(String.valueOf(i)).child("long").getValue(Double.class));
                                    waypoints.add(waypoint);
                                    Log.e("waypoint "+String.valueOf(i),String.valueOf(waypoint));
                                    mMap.addMarker(new MarkerOptions()
                                            .position(waypoint)
                                            .title("Waypoint")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                }

                            }
                            String url = getDirectionsUrl(origin,destination,waypoints);
                            Log.e("URL",url);
                            DownloadTask downloadTask = new DownloadTask();

                            // Start downloading json data from Google Directions API
                            downloadTask.execute(url);
                        }
                    }




                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private String getDirectionsUrl(LatLng origin, LatLng dest,ArrayList<LatLng> waypoints) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String waypointStr = "waypoints=";
        for(int i=0;i<waypoints.size();i++){
            LatLng point  = (LatLng) waypoints.get(i);

            waypointStr += point.latitude + "," + point.longitude + "|";
        }

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+waypointStr+"&"+"key="+"AIzaSyBWjDqtqfCJSiXSkA73kgC4PBlbXDGTo8M";


        // Building the parameters to the web service
       // String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }



    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.e("Exception while url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service

            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.e("Data",String.valueOf(data));
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                Log.e("Coming through","Results");
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.BLUE);
            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }


















    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
       getData();
    }
}

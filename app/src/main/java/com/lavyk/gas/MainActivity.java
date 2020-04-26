package com.lavyk.gas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import hu.supercluster.overpasser.adapter.OverpassQueryResult;
import hu.supercluster.overpasser.adapter.OverpassServiceProvider;
import hu.supercluster.overpasser.adapter.OverpassQueryResult.Element;

public class MainActivity extends AppCompatActivity {
    private MapView map = null;
    private TextView txtCity, txtCoord, txtLatitude, txtLongitude, txtDistance;
    private Button btnStart;
    private boolean detachedMode;
    private Location loc = null;
    private String cityName = null;
    private Handler mHandler;
    private Object mHandlerToken = new Object();
    private final LinkedList<Runnable> mRunOnFirstFix = new LinkedList<Runnable>();
    private Location mLocation;
    private GeoPoint mGeoPoint;
    private IMapController mapController;
    private GeocoderNominatim nominatim;
    private Runnable updater;
    final Handler timerHandler = new Handler();
    private LocationListener locationListener;
    private Map<Long, Element> poiMap;
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLatitude = (TextView) findViewById(R.id.txtLatitude);
        txtLongitude = (TextView) findViewById(R.id.txtLongitude);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        btnStart =  findViewById(R.id.btnStart);

        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Toast.makeText(MainActivity.this, "Stttart", Toast.LENGTH_SHORT).show();
                intent = new Intent(MainActivity.this, GpsService.class);
                startService(intent);
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ) {

            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
        } else {
            // Permission has already been granted
        }

        pedirPermissoes();

        Context ctx = getApplicationContext();
        locationListener = new MyLocationListener(ctx);
        Locale local = getResources().getConfiguration().locale;

        nominatim = new GeocoderNominatim(local, "");


        map = (MapView) findViewById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }


        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

/*        txtCity = (TextView) findViewById(R.id.textCity);
        loc = ((MyLocationListener) locationListener).getLocation();
        cityName = ((MyLocationListener) locationListener).getCity();*/

        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });


        GpsMyLocationProvider prov= new GpsMyLocationProvider(this);
        prov.addLocationSource(LocationManager.NETWORK_PROVIDER);
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(prov, map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        //Bitmap icon = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_emoji_people_black_24dp);
        //locationOverlay.setPersonIcon(icon);

        map.setTileSource(TileSourceFactory.MAPNIK);

        mapController = map.getController();
        mapController.setZoom(17f);

//        GeoPoint startPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
//        textCity.setText(cityName);

        map.getOverlayManager().add(locationOverlay);

        //updateCityNameTextView(new GeoPoint(getLastKnownLocation().getLatitude(), getLastKnownLocation().getLongitude()));


        //mapController.setCenter(locationOverlay.getMyLocation());
        map.setClickable(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMultiTouchControls(true);
        //textCity.setText("Pos :" +  getLastKnownLocation().getLatitude());

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String distance = intent.getStringExtra("distance");
            txtDistance.setText("Distance is " + distance+" M" );
        }
    };

    public void onResume(){
        super.onResume();
        map.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(GpsService.BROADCAST_ACTION)); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause() {
        super.onPause();
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(updater);
    }

    public void getAddressFromLocation(final double latitude, final double longitude, final Context context) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        String result = null;
                        try {
                            List<Address> addressList = geocoder.getFromLocation(
                                    latitude, longitude, 1);
                            if (addressList != null && addressList.size() > 0) {
                                Address address = addressList.get(0);
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                                    sb.append(address.getAddressLine(i)).append("\n");
                                }
                                System.out.println(address.getUrl());
                                sb.append("URL: " + address.getUrl()).append("\n");
                                sb.append(address.getSubLocality()).append("\n");

                                sb.append(address.getPostalCode()).append("\n");
                                sb.append(address.getCountryName());
                                result = sb.toString();
                            }
                        } catch (IOException e) {
                            Log.e("GeoCondingAndress", "Unable connect to Geocoder", e);
                        } finally {
                            if (result != null) {
                                result = "Latitude: " + latitude + " Longitude: " + longitude +
                                        "\n\nAddress:\n" + result;
                            } else {
                                result = "Latitude: " + latitude + " Longitude: " + longitude +
                                        "\n Unable to get address for this lat-long.";
                            }
                            //textCity.setText(result);
                        }
                    }
                    });

            }
        };
        thread.start();
    }

    /*public void getAddress(GeoPoint p) {
        final double dLatitude = p.getLatitude();
        final double dLongitude = p.getLongitude();

        String theAddress = null;
        GeocoderNominatim geocoder = new GeocoderNominatim("");
                try {
                    List<Address> addresses = geocoder.getFromLocation(dLatitude,
                            dLongitude, 1);
                    StringBuilder sb = new StringBuilder();
                    textCoord.setText(dLatitude + " / " + dLongitude);
                    if (addresses.size() > 0) {
                        Address address = addresses.get(0);
                        int n = address.getMaxAddressLineIndex();
                        for (int i = 0; i <= n; i++) {
                            if (i != 0)
                                sb.append(", ");
                            sb.append(address.getAddressLine(i));
                        }
                        theAddress = new String(sb.toString());
                        textCity.setText(theAddress);
                    } else {
                        textCity.setText("Erro");
                    }
                } catch (IOException e) {
                    textCity.setText("Erro");
                }


    }*/


    private void pedirPermissoes() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        else
            configurarServico();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configurarServico();
                } else {
                    Toast.makeText(this, "Não vai funcionar!!!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void configurarServico(){
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    atualizar(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) { }

                public void onProviderEnabled(String provider) { }

                public void onProviderDisabled(String provider) {
                    Toast.makeText(MainActivity.this, provider + " foi desabilitado", Toast.LENGTH_LONG).show();
                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }catch(SecurityException ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void atualizar(Location location)
    {
        Double latPoint = location.getLatitude();
        Double lngPoint = location.getLongitude();

        txtLatitude.setText(latPoint.toString());
        txtLongitude.setText(lngPoint.toString());

        //getAddressFromLocation(latPoint, lngPoint, this);

        /*try {
            Document doc = getNodesViaOverpass("A");
            //textCity.setText(doc.getDocumentURI());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        OverpassQuery query = new OverpassQuery()
                .format(JSON)
                .timeout(30)
                .filterQuery()
                .node()
                .amenity("fuel")
                .boundingBox( -7.298279,-36.007175,-7.191848,-35.775604
                )
                .end()
                .output(100)
                ;

        String query2 = new OverpassQuery()
                .format(JSON)
                .timeout(30)
                .filterQuery()
                .node()
                .amenity("parking")
                .tagNot("access", "private")
                .boundingBox(
                        47.48047027491862, 19.039797484874725,
                        47.51331674014172, 19.07404761761427
                )
                .prepareNext()
                .way()
                .amenity("parking")
                .tagNot("access", "private")
                .boundingBox(
                        47.48047027491862, 19.039797484874725,
                        47.51331674014172, 19.07404761761427
                )
                .prepareNext()
                .rel()
                .amenity("parking")
                .tagNot("access", "private")
                .boundingBox(
                        47.48047027491862, 19.039797484874725,
                        47.51331674014172, 19.07404761761427
                )
                .end()
                .output(OutputVerbosity.BODY, OutputModificator.CENTER, OutputOrder.QT, 100)
                .build()
                ;

        OverpassQueryResult result = interpret(query.build());

        if (result != null) {
            for (Element poi : result.elements) {
                if (!alreadyStored(poi)) {
                    fixTitle(poi);
                    storePoi(poi);
                    showPoi(poi);
                }
            }
        }*/
    }

    private OverpassQueryResult interpret(String query) {
        try {
            return OverpassServiceProvider.get().interpreter(query).execute().body();

        } catch (Exception e) {
            e.printStackTrace();

            return new OverpassQueryResult();
        }
    }

    private boolean alreadyStored(Element poi) {
        return poiMap.containsKey(poi.id);
    }

    private void fixTitle(Element poi) {
        Element.Tags info = poi.tags;

        if (info.name == null) {
            info.name = "fuel";
        }
    }

    private void storePoi(Element poi) {
        poiMap.put(poi.id, poi);
    }

    void showPoi(Element poi) {
        txtCity.append("latPosto: " + poi.lat + " / " + poi.lon + "\n");
/*        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(poi.lat, poi.lon))
                ;

        Marker marker = fragment.getGoogleMap().addMarker(options);
        poiInfoWindowAdapter.addMarkerInfo(marker, poi);*/
    }

    public Document getNodesViaOverpass(String query, Context context) throws IOException, ParserConfigurationException, SAXException {
        final Document[] doc = {null};
        Thread threadOverpass = new Thread() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String hostname = "http://www.overpass-api.de/api/interpreter";
                        String queryString = "[out:\"json\"][timeout:\"30\"];"
                                + "("
                                + "     node[\"amenity\"=\"fuel\"](-7.232252,-35.903320,-7.205643,-35.845428);"
                                + "     way[\"amenity\"=\"fuel\"](-7.232252,-35.903320,-7.205643,-35.845428);"
                                + "     rel[\"amenity\"=\"fuel\"](-7.232252,-35.903320,-7.205643,-35.845428);"
                                + ");"
                                + "out body center qt 100;";
                        try {
                            URL osm = new URL(hostname);
                            HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                            DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
                            printout.writeBytes("data=" + URLEncoder.encode(queryString, "utf-8"));
                            printout.flush();
                            printout.close();

                            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
                            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();

                            doc[0] =  docBuilder.parse(connection.getInputStream());
                        } catch (Exception e) {

                        }
                    }

                });
            }
        };
        threadOverpass.start();
        return doc[0];
    }
}

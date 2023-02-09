package com.blackbox.pinspot.ui.main;

import static android.content.ContentValues.TAG;

import static com.blackbox.pinspot.util.Constants.LAST_LAT;
import static com.blackbox.pinspot.util.Constants.LAST_LON;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.blackbox.pinspot.R;
import com.blackbox.pinspot.data.repository.pin.IPinRepository;
import com.blackbox.pinspot.databinding.FragmentMapBinding;
import com.blackbox.pinspot.model.Pin;
import com.blackbox.pinspot.util.ServiceLocator;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.firebase.geofire.GeoLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    public class MyGoToLoginAction implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Navigation.findNavController(v).navigate(R.id.action_mapFragment_to_loginActivity);
            // Code to undo the user's last action
        }
    }
    Double startLat = 0.0;
    Double startLon = 0.0;
    Double currCameraLat = 0.0;
    Double currCameraLon = 0.0;
    Double lastUpdateLat = 0.0;
    Double lastUpdateLon = 0.0;

    Double lastSavedLat = 0.0;
    Double lastSavedLon = 0.0;
    private MapViewModel mapViewModel;
    private FragmentMapBinding binding;
    PinViewModel pinViewModel;

    private ActivityResultLauncher<String[]> multiplePermissionLauncher;
    private ActivityResultContracts.RequestMultiplePermissions multiplePermissionsContract;
    private final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationProviderClient fusedLocationClient;
    /*private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 20;
    private Location lastKnownLocation;*/
    private LatLng mypos = new LatLng(0,0);
    private ArrayList<Marker> markers = new ArrayList<Marker>();

    private GoogleMap googleMap;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        IPinRepository pinRepository =
                ServiceLocator.getInstance().getPinRepository(requireActivity().getApplication());

        if (pinRepository != null) {
            // This is the way to create a ViewModel with custom parameters
            // (see NewsViewModelFactory class for the implementation details)
            pinViewModel = new ViewModelProvider(
                    requireActivity(),
                    new PinViewModelFactory(pinRepository)).get(PinViewModel.class);
        } else {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    getString(R.string.unexpected_error), Snackbar.LENGTH_SHORT).show();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        Toast.makeText(requireActivity(),
                "connessione: "+Boolean.toString(isOnline()) ,
                Toast.LENGTH_SHORT).show();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        //fragmentMapBinding.mapv.getMapAsync(this);

        multiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();

        multiplePermissionLauncher = registerForActivityResult(multiplePermissionsContract, isGranted -> {
            for(Map.Entry<String, Boolean> set : isGranted.entrySet()) {
                Log.d(TAG, set.getKey() + " " + set.getValue());
            }
            if (!isGranted.containsValue(false)) {
                Log.d(TAG, "All permission have been granted");
                getDeviceLocation(googleMap);
            }
        });

        binding = FragmentMapBinding.inflate(inflater, container, false);



        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(savedInstanceState != null){

            lastSavedLat = mapViewModel.getLastLat();
            lastSavedLon = mapViewModel.getLastLon();

        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        multiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();

        MapView mapView = view.findViewById(R.id.mapv);
        mapView.onCreate(savedInstanceState);

        mapView.onResume();
        mapView.getMapAsync(this);

        /*fragmentMapBinding.mapv.onCreate(savedInstanceState);
        fragmentMapBinding.mapv.onResume();
        fragmentMapBinding.mapv.getMapAsync(this);*/
        ActivityResultLauncher<Intent> insertPinActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Toast.makeText(requireActivity(),
                                " yeeeeee" ,
                                Toast.LENGTH_SHORT).show();
                        googleMap.clear();
                        getDeviceLocation(googleMap);

                        updatePin(googleMap, startLat, startLon);
                        //}
                    }
                });

            binding.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences sharedPref = requireActivity().getSharedPreferences(
                            "settings", Context.MODE_PRIVATE); //TODO DAMETTEREINUNACOSTANTE
                    Boolean skipSettings = sharedPref.getBoolean("skip", false);
                    if (skipSettings == false) {

                        //TODO Pass forward current latitude and longitude to the InsertPinActivity
                        getDeviceLocation(googleMap);


                        Intent intent = new Intent(requireContext(), InsertPinActivity.class);
                        intent.putExtra("latitude", mypos.latitude);
                        intent.putExtra("longitude", mypos.longitude);
                        //startActivity(intent);
                        insertPinActivityResultLauncher.launch(intent);

                    }else{
                        Snackbar.make(v,
                                "Vai al login",
                                Snackbar.LENGTH_SHORT).setAction("vai", new MyGoToLoginAction())
                                .show();

                    }
                }
            });

        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            Place place = Autocomplete.getPlaceFromIntent(data);
                            LatLng target = place.getLatLng();
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 14.0f));

                        }
                    }
                });

        //COSTANTE AGGIUNGERECOSTANTI IMBECILLI SE NON L'ABBBIAMO ANCORA FATTO
        Places.initialize(requireContext(), "AIzaSyBVzu-lEm7gs-V1AElIWVgwHlNXdaeuVyM");
        binding.searchButton.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);

            // Start the autocomplete intent.
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(requireContext());
            someActivityResultLauncher.launch(intent);
        });
    }

    private Marker marker;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setOnMarkerClickListener(this);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                /*MarkerOptions marker = new MarkerOptions().position(
                                latLng)
                        .title("Hello Maps ");
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                googleMap.addMarker(marker);*/
            }

        });

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation(googleMap);
            return;
        }
        // Turn on the My Location layer and the related control on the map.

        getDeviceLocation(googleMap);
        if (googleMap != null) {

            googleMap.clear();
            updatePin(googleMap,lastSavedLat, lastSavedLon);
        }
        googleMap.setMyLocationEnabled(true);

        googleMap.setMaxZoomPreference(20.0f);
        googleMap.setMinZoomPreference(13.5f);

        /*googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override public void onCameraChange(CameraPosition cameraPosition) {
                // camera change can occur programmatically.
                if (isResumed()) {
                    Toast.makeText(requireActivity(),
                            " MISTOMUOVENDO" ,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });*/

        googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                currCameraLat = googleMap.getCameraPosition().target.latitude;
                currCameraLon = googleMap.getCameraPosition().target.longitude;
                if(((currCameraLat - lastUpdateLat)>= 0.01) || ((currCameraLon - lastUpdateLon)>= 0.01)){
                    lastUpdateLat = currCameraLat;
                    lastUpdateLon = currCameraLon;
                    Toast.makeText(requireActivity(),
                            " MISTOMUOVENDO" ,
                            Toast.LENGTH_SHORT).show();
                    if (isResumed()) {
                        if (googleMap != null) {
                            for (int i = 0; i < markers.size(); ++i)
                            {
                                markers.get(i).remove();
                            }
                            markers.clear();
                            googleMap.clear();
                            updatePin(googleMap,currCameraLat, currCameraLon);
                        }

                    }
                }

            }
        });
        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener()
        {
            @Override
            public boolean onMyLocationButtonClick() {
                Toast.makeText(requireActivity(),
                        " bottonecliccato" ,
                        Toast.LENGTH_SHORT).show();
                googleMap.clear();
                updatePin(googleMap, startLat, startLon);
                // Return false so that we don't consume the event and the default behavior still occurs
                // (the camera animates to the user's current position).
                return false;
            }
        });

        /*markerlist.add(new myMarker(45.01, 9, "pin1", "id1"));
        markerlist.add(new myMarker(45.02, 9, "pin2", "id2"));
        markerlist.add(new myMarker(45.03, 9, "pin3", "id3"));
        int size =  markerlist.size();
        for(int i =0 ;i<size;++i) {
            //POSSIBILE INIZIO METODO ISTANZIAZIONE MARKER

            LatLng markerpos = new LatLng(markerlist.get(i).getLat(), markerlist.get(i).getLon());
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(markerpos)
                    .title(markerlist.get(i).getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))//cambio colore IMPORT NECESSARIO
                    //BitmapDescriptorFactory.fromResource(R.drawable.arrow) cosi possiamo mettere la mappa
                    .alpha(0.9f)//cambio opacità
                    .flat(true)//In teoria dovremmo averlo così ma bho non cambia nulla a prima vista
            );
            //marker.setTag(i);*/



        //markerlist.add(new myMarker(45.01, 9, "pin1", "id1"));

        /*int size = markerlist.size();
        String s = "I risultati sono: "+Integer.toString(size);
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();

        for(int i = 0;i < size;++i) {
            LatLng markerpos = new LatLng(markerlist.get(i).getLat(), markerlist.get(i).getLon());
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(markerpos)
                    .title(markerlist.get(i).getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))//cambio colore IMPORT NECESSARIO
                    //BitmapDescriptorFactory.fromResource(R.drawable.arrow) cosi possiamo mettere la mappa
                    .alpha(0.9f)//cambio opacità
                    .flat(true)//In teoria dovremmo averlo così ma bho non cambia nulla a prima vista
            );
            marker.setTag(markerlist.get(i).getIdPin());
        }*/


        /*LatLng markerpos = new LatLng(45.01, 9);
        marker = googleMap.addMarker(new MarkerOptions()
                .position(markerpos)
                .title("sono un test")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))//cambio colore IMPORT NECESSARIO
                //BitmapDescriptorFactory.fromResource(R.drawable.arrow) cosi possiamo mettere la mappa
                .alpha(0.9f)//cambio opacità
                .flat(true)//In teoria dovremmo averlo così ma bho non cambia nulla a prima vista
        );*/

    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        lastSavedLat = googleMap.getCameraPosition().target.latitude;
        lastSavedLon = googleMap.getCameraPosition().target.longitude;
        if (marker.getTag() == null){
            Toast.makeText(requireActivity(), "Vuoto", Toast.LENGTH_SHORT).show();
        } else {

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            DocumentReference docRef = db.collection("pins4").document(marker.getTag().toString());

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    Double latitude, longitude;
                    String title, link;
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            title = document.getString("title");
                            latitude = document.getDouble("lat");
                            longitude = document.getDouble("lon");
                            link = document.getString("link");

                            if (latitude != null && longitude != null){
                                Pin pin = new Pin(latitude, longitude, title, link);
                                MapFragmentDirections.ActionMapFragmentToPinInfoFragment action =
                                        MapFragmentDirections.
                                                actionMapFragmentToPinInfoFragment(pin);
                                Navigation.findNavController(requireView()).navigate(action);
                            }

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }

        return false;
    }


    private void getDeviceLocation(GoogleMap map){
        // Using a boolean variable to determine if permissions have been granted
        boolean multiplePermissionsStatus =
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        try {
            if (multiplePermissionsStatus) {

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(),  new OnSuccessListener<Location>() {
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    Log.d(TAG, location.getLatitude() + " " + location.getLongitude());
                                    LatLng currentPos = new LatLng(location.getLatitude(), location.getLongitude());
                                    startLat = currentPos.latitude;
                                    startLon = currentPos.longitude;
                                    updatePin(googleMap, startLat, startLon);
                                    if(lastSavedLat!=0.0||lastSavedLon!=0.0){
                                        mypos = new LatLng(lastSavedLat,lastSavedLon);
                                    }else{
                                        mypos = currentPos;
                                    }
                                    lastSavedLat=startLat;
                                    lastSavedLon=startLon;
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(mypos, 14.0f));
                                    map.setMyLocationEnabled(true);
                                }
                            }
                        });

                /*Task<Location> locationResult = fusedLocationClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                fragmentMapBinding.mapv.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });*/
            } else {
                multiplePermissionLauncher.launch(PERMISSIONS);
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }


    public void updatePin(GoogleMap map, Double radiusLat, Double radiusLon){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        //final GeoLocation center = new GeoLocation(45.830308, 8.645078);
        final GeoLocation center = new GeoLocation(radiusLat, radiusLon);
        final double radiusInM = 10 * 1000;

        // Each item in 'bounds' represents a startAt/endAt pair. We have to issue
        // a separate query for each pair. There can be up to 9 pairs of bounds
        // depending on overlap, but in most cases there are 4.

        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("pins4")
                    .orderBy("geoHash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());
        }

        // Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Task<?>>> t) {
                        List<DocumentSnapshot> matchingDocs = new ArrayList<>();

                        for (Task<QuerySnapshot> task : tasks) {
                            QuerySnapshot snap = task.getResult();
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                double lat = doc.getDouble("lat");
                                double lng = doc.getDouble("lon");

                                // We have to filter out a few false positives due to GeoHash
                                // accuracy, but most will match
                                GeoLocation docLocation = new GeoLocation(lat, lng);
                                double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                                if (distanceInM <= radiusInM) {
                                    matchingDocs.add(doc);
                                }
                            }
                        }
                        int size = matchingDocs.size();

                        String s = "I risultati sono: "+Integer.toString(size);
                        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();

                        for(int i = 0 ;i<size;++i){
                            double lat =  matchingDocs.get(i).getDouble("lat");
                            double lon =  matchingDocs.get(i).getDouble("lon");
                            String title = matchingDocs.get(i).getString("title");
                            String idpin = matchingDocs.get(i).getId();

                            //Put pins on map
                            marker = map.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lon))
                                    .title(title)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))//cambio colore IMPORT NECESSARIO
                                    //BitmapDescriptorFactory.fromResource(R.drawable.arrow) cosi possiamo mettere la mappa
                                    .alpha(0.9f)//cambio opacità
                                    .flat(true)//In teoria dovremmo averlo così ma bho non cambia nulla a prima vista
                            );
                            marker.setTag(idpin);
                            markers.add(marker);
                        }
                    }
                });
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Toast.makeText(requireActivity(),
                " pausa",
                Toast.LENGTH_SHORT).show();
        mapViewModel.setLastLat(googleMap.getCameraPosition().target.latitude);
        mapViewModel.setLastLon(googleMap.getCameraPosition().target.longitude);
    }
}
package com.blackbox.pinspot.ui.main;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.blackbox.pinspot.R;
import com.blackbox.pinspot.data.repository.weather.IWeatherRepositoryWithLiveData;
import com.blackbox.pinspot.databinding.FragmentPinInfoBinding;
import com.blackbox.pinspot.model.Result;
import com.blackbox.pinspot.model.weather.WeatherApiResponse;
import com.blackbox.pinspot.util.ServiceLocator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PinInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PinInfoFragment extends Fragment {

    private WeatherViewModel weatherViewModel;

    private FragmentPinInfoBinding binding;
    //private String apikey = "4f6ec18ab9eb724adb869edca9cbbf63";

    public PinInfoFragment() {
        // Required empty public constructor
    }

    public static PinInfoFragment newInstance() {
        return new PinInfoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        IWeatherRepositoryWithLiveData weatherRepositoryWithLiveData =
                ServiceLocator.getInstance().getWeatherRepository(requireActivity().getApplication());

        if (weatherRepositoryWithLiveData != null) {
            // This is the way to create a ViewModel with custom parameters
            // (see NewsViewModelFactory class for the implementation details)
            weatherViewModel = new ViewModelProvider(
                    requireActivity(),
                    new WeatherViewModelFactory(weatherRepositoryWithLiveData)).get(WeatherViewModel.class);
        } else {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    getString(R.string.unexpected_error), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPinInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    Navigation.findNavController(requireView()).navigateUp();
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        binding.closePinInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        String pinID = PinInfoFragmentArgs.fromBundle(getArguments()).getPinID();
        if (pinID != null){
            binding.PinTitleTextView.setText(pinID);

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            DocumentReference docRef = db.collection("pins4").document(pinID);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    Double latitude, longitude;
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            latitude = document.getDouble("lat");
                            longitude = document.getDouble("lon");
                            binding.PinLatTextView.setText(String.valueOf(latitude));
                            binding.pinLongTextView.setText(String.valueOf(longitude));

                            weatherViewModel.getPinWeather(latitude, longitude).observe(getViewLifecycleOwner(),
                                    result -> {
                                        if (result.isSuccess()){
                                            WeatherApiResponse weatherApiResponse = ((Result.WeatherResponseSuccess) result).getData();
                                            Double temp = weatherApiResponse.getMainWeatherInfo().getTemp();
                                            String description = weatherApiResponse.getWeather()[0].getDescription();

                                            //TODO solo a scopo di test, ma vanno aggiunte altre textview
                                            //binding.PinLatTextView.setText(String.valueOf(temp));
                                            //binding.pinLongTextView.setText(description);
                                            Integer temperature = (int) (temp - 273.15);
                                            SharedPreferences sharedPref = requireContext().getSharedPreferences(
                                                    "settings", Context.MODE_PRIVATE); //DAMETTEREINUNACOSTANTE
                                            Boolean celsiusSettings = sharedPref.getBoolean("celsius", true);
                                            if(celsiusSettings == true){
                                                binding.textViewTemperature.setText(String.valueOf(temperature) + " °C");
                                            }else{
                                                binding.textViewTemperature.setText(String.valueOf(celsToFar(temperature)) + " °F");
                                            }
                                            //binding.textViewTemperature.setText(String.valueOf(temp));
                                            binding.textViewWeatherDescription.setText(description);

                                        } else {
                                            Toast.makeText(requireContext(), "Errore imprevisto", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });



            /*db.collection("pins4")
                    .get(Source.valueOf(pinID))
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            String result = "", title = "", latitude = "", longitude = "";
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    result += document.getData();
                                    title += document.getString("title") + " ";
                                    latitude += document.getString("lat") + " ";
                                    longitude += document.getString("lon") + " ";
                                    //pos += document.get("position");

                                    //firstPinTitleValue.setText(document.get("title").toString());
                                }
                                //fragmentPinInfoBinding.PinTitleTextView.setText(title);
                                fragmentPinInfoBinding.PinLatTextView.setText(latitude);
                                fragmentPinInfoBinding.pinLongTextView.setText(longitude);
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });*/
        } else{
            binding.PinTitleTextView.setText("NULL");
        }

        /*Double latitude = 45.830736;
        Double longitude = 8.646034;
        weatherViewModel.getPinWeather(latitude, longitude).observe(getViewLifecycleOwner(),
                result -> {
                    if (result.isSuccess()){
                        WeatherApiResponse weatherApiResponse = ((Result.WeatherResponseSuccess) result).getData();
                        Double temp = weatherApiResponse.getMainWeatherInfo().getTemp();
                        String description = weatherApiResponse.getWeather()[0].getDescription();

                        //TODO solo a scopo di test, ma vanno aggiunte altre textview
                        //binding.PinLatTextView.setText(String.valueOf(temp));
                        //binding.pinLongTextView.setText(description);

                        binding.textViewTemperature.setText(String.valueOf(temp));
                        binding.textViewWeatherDescription.setText(description);

                    } else {
                        Toast.makeText(requireContext(), "Errore imprevisto", Toast.LENGTH_SHORT).show();
                    }
                });*/

    }

    private int celsToFar(int c){
        return (int) ((c * 1.8) + 32);
    }
}
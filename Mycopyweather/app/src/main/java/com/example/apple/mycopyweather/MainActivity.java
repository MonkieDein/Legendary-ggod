package com.example.apple.mycopyweather;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.apple.mycopyweather.model.DailyWeatherReport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.lang.Math.abs;

public class MainActivity  extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,LocationListener {

    final String URL_BASE="http://api.openweathermap.org/data/2.5/forecast";
    final String URL_COORD="/?lat=";//"/?lat=9.9687&lon=76.299";
    final String URL_UNITS ="&units=metric";
    final String URL_API_KEY="&APPID=5b8827191af217f2b6e197771c0b033b";

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSION_LOCATION = 111;
/*    private FusedLocationProviderClient mFusedLocationClient;*/
    private ArrayList<DailyWeatherReport> weatherReportList = new ArrayList<>();
    private ArrayList<DailyWeatherReport> weatherReportList2 = new ArrayList<>();

    ImageButton weatherIconMini;
    private ImageView weatherIcon;
    private TextView weatherDate;
    private TextView weatherDate2;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;
    private TextView rawDate;
    EditText yearInput;
    EditText monthInput;
    EditText dayInput;
    EditText hourInput;
    EditText minuteInput;
    EditText secondInput;
    public long timestampInput;
    String timestampstring,year,month,day,hour,minute,second;
    long timestampnow,timestampbefore;
    WeatherAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weatherIconMini = (ImageButton)findViewById(R.id.weatherIconMini);
        weatherIcon = (ImageView)findViewById(R.id.weatherIcon);
        weatherDate = (TextView)findViewById(R.id.weatherDate);
        currentTemp = (TextView)findViewById(R.id.currentTemp);
        lowTemp = (TextView)findViewById(R.id.lowTemp);
        cityCountry = (TextView)findViewById(R.id.cityCountry);
        weatherDescription=(TextView)findViewById(R.id.weatherDescription);
        rawDate = (TextView)findViewById(R.id.weatherDate);
        yearInput = (EditText) findViewById(R.id.Year);
        monthInput = (EditText) findViewById(R.id.Month);
        dayInput = (EditText) findViewById(R.id.Day);
        hourInput = (EditText) findViewById(R.id.Hour);
        minuteInput = (EditText) findViewById(R.id.Minute);
        secondInput = (EditText) findViewById(R.id.Second);
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.content_weather_reports);

        mAdapter=new WeatherAdapter(weatherReportList);

        recyclerView.setAdapter(mAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);

        recyclerView.setLayoutManager(layoutManager);

/*        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this);*/
        mGoogleApiClient= new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

//        downloadWeatherData(null);

// this image is set as an button like a search button
        //there is a bug in this function the card view is only able to change once.
        weatherIconMini.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)  {
                year= yearInput.getText().toString();
                month= monthInput.getText().toString();
                day= dayInput.getText().toString();
                hour= hourInput.getText().toString();
                minute= minuteInput.getText().toString();
                second= secondInput.getText().toString();
                timestampstring= year + "-" + month + "-" + day + "T" + hour + ":" + minute + ":" + second + "GMT"  ;
                Log.v("TIME given", "TIME=" + timestampstring);

                try {
                    timestampInput= searchingweather(timestampstring);
                } catch (ParseException e) {
                    e.printStackTrace();
                    Log.v("TIME given", "there is an error");
                }
                Log.v("TIME given", "TIME=" + timestampInput);
                clearview();

            }
        });
    }
// this function act as input to search for specific weather of the time given by user, it change the time to timestamp (long)
    public long searchingweather(String datestring) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        Date dateint = dateFormat.parse(datestring);
        Log.v("TIME given","dateint is " +dateint);
        long unixTime = (long) dateint.getTime()/1000;

        Calendar ca = GregorianCalendar.getInstance();
        GregorianCalendar cal = new GregorianCalendar(ca.get(Calendar.YEAR),ca.get(Calendar.MONTH),ca.get(Calendar.DAY_OF_MONTH));
        long adjustment=cal.get(GregorianCalendar.ZONE_OFFSET);

        unixTime = unixTime - adjustment/1000;

        Log.v("TIME given", "dt : " + unixTime);
        Log.v("closest", "dt : " + unixTime);
        return unixTime;
    }
// this function is act as the on click function that change the image shown and the card veiw.
    //there is a bug in this function the card view is only able to change once.
    public void clearview(){
        weatherReportList2=(ArrayList<DailyWeatherReport>)weatherReportList.clone();
        int closest = 0;
        int x = this.weatherReportList.size();
//        this.weatherReportList.add(weatherReportList2.get(closest));
//        Log.v("closest", "closest: " +closest);
//        mAdapter=new WeatherAdapter(weatherReportList);

        updateUI();
        mAdapter.notifyDataSetChanged();

        DailyWeatherReport report = weatherReportList.get(0);
        timestampbefore=report.getweatherDate();
        Log.v("try variable","variable:"+ timestampbefore);

//        report = weatherReportList.get(1);
//        timestampnow=report.getweatherDate();
//        long thisdif=(abs(timestampnow-timestampInput));
//        Log.v("try variable","variable:"+ thisdif);

        for (int i = 1; i < x; i++){

            report = weatherReportList.get(i);
            timestampnow=report.getweatherDate();
            long thisdif=(abs(timestampnow-timestampInput));
            long oridif=(abs(timestampbefore-timestampInput));
            if (thisdif<oridif){
                closest=i;
                timestampbefore=timestampnow;

                Log.v("try variable","variable:"+ timestampnow);
            }
            Log.v("closest", "timeStampinput:" +timestampInput);
            Log.v("closest","thisdif: "+String.valueOf(thisdif)+"   oridif: "+String.valueOf(oridif)+" closest:  "+closest);
            Log.v("try variable","variable print x:"+ x);
        }
        if (x>1 && timestampInput>1000000000){
            for (int i = (x-1); i >= 0; i=i-1){
                if(i!=closest){
                    Log.v("closest", "closest and index: " +closest + "   "+i);
                    this.weatherReportList.remove(i);
                    mAdapter.notifyItemRangeRemoved(i,x);
                }
            }
        }
        int newlen = this.weatherReportList.size();
        Log.v("closest", "new length: " +newlen);

        updateUI();
        mAdapter.notifyDataSetChanged();

        this.weatherReportList.remove(0);
        mAdapter.notifyItemRangeRemoved(0,x);
//

//        this.weatherReportList.add(weatherReportList2.get(closest));
//        Log.v("closest", "closest: " +closest);

        updateUI();
        mAdapter.notifyDataSetChanged();

        weatherReportList=(ArrayList<DailyWeatherReport>)weatherReportList2.clone();


    }

// this function is used to download weather data frm openweather.api given object type location.
    public void downloadWeatherData(Location location){
        Log.v("Lockation", "location i have is " + location);
//        Lockation is "Location[fused 43.134411,-70.934827 acc=19 et=+3h53m0s92ms alt=2.700000047683716]".
//        final String url= "http://api.openweathermap.org/data/2.5/forecast/?lat=/?lat=9.9687&lon=76.299&units=metric&APPID=5b8827191af217f2b6e197771c0b033b";
//        System.out.print(url);
        final String fullCoords = URL_COORD+location.getLatitude() + "&lon=" + location.getLongitude();
//        final String fullCoords = URL_COORD+"9.9687" + "&lon=" + "76.299";
        final String url= URL_BASE + fullCoords + URL_UNITS + URL_API_KEY;
        Log.v("FUN2","URL:"+ url);
//        final String url= "https://pokeapi.co/api/v2/pokemon/";
        final JsonObjectRequest jsonRequest= new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Log.v("FUN","RES: " + response.toString());
                try {
                    JSONObject city = response.getJSONObject("city");
                    String cityName = city.getString("name");
                    String country = city.getString("country");

                    JSONArray list = response.getJSONArray("list");

                    //i edit
//                    JSONObject obj = list.getJSONObject(0);
//                    JSONObject main = obj.getJSONObject("main");
//                    Double currentTemp = main.getDouble("temp");
//                    Double maxTemp = main.getDouble("temp_max");
//                    Double minTemp = main.getDouble("temp_min");
//
//                    JSONArray weatherArr = obj.getJSONArray("weather");
//                    JSONObject weather = weatherArr.getJSONObject(0);
//                    String weatherType = weather.getString("main");
//
//                    String rawDate=obj.getString("dt_txt");
//                    rawDate=rawDate.substring(0,10);
//                    Integer weatherDate = (int) obj.getDouble("dt");

                    int closest = 0;

                    for (int x = 0; x < 37; x++){
//                        JSONObject obj = list.getJSONObject(x);
//                        long weatherDate = (long) obj.getDouble("dt");
//                        Log.v("closest", "weatherDate:" + weatherDate );
//                        long thisdif=(abs(weatherDate-timestampInput));
//                        long oridif=(abs((((long) (list.getJSONObject(closest).getDouble("dt"))) - timestampInput)));
//                        if (thisdif<oridif){
//                            closest=x;
//                        }
//                        Log.v("closest", "timeStampinput:" +timestampInput);
//                        Log.v("closest","thisdif: "+String.valueOf(thisdif)+"   oridif: "+String.valueOf(oridif));
//                    }
//                    Log.v("closest", "the best date:" + closest );
//                    JSONObject obj = list.getJSONObject(closest);
//                    JSONObject main = obj.getJSONObject("main");
//                    Double currentTemp = main.getDouble("temp");
//                    Double maxTemp = main.getDouble("temp_max");
//                    Double minTemp = main.getDouble("temp_min");
//
//                    JSONArray weatherArr = obj.getJSONArray("weather");
//                    JSONObject weather = weatherArr.getJSONObject(0);
//                    String weatherType = weather.getString("main");
//
//                    String rawDate=obj.getString("dt_txt");
//                    rawDate=rawDate.substring(0,10);
//                    Integer weatherDate = (int) obj.getDouble("dt");
                        JSONObject obj = list.getJSONObject(x);
                        JSONObject main = obj.getJSONObject("main");
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");

                        JSONArray weatherArr = obj.getJSONArray("weather");
                        JSONObject weather = weatherArr.getJSONObject(0);
                        String weatherType = weather.getString("main");

                        String rawDate=obj.getString("dt_txt");
                        //rawDate=rawDate.substring(0,10);
                        Long weatherDate = (long) obj.getDouble("dt");
                        //take second object and compare
//                        JSONObject obj2 = list.getJSONObject(x);
//                        JSONObject main2 = obj2.getJSONObject("main");
//                        Double currentTemp2 = main2.getDouble("temp");
//                        Double maxTemp2 = main2.getDouble("temp_max");
//                        Double minTemp2 = main2.getDouble("temp_min");
//
//                        JSONArray weatherArr2 = obj2.getJSONArray("weather");
//                        JSONObject weather2 = weatherArr2.getJSONObject(0);
//                        String weatherType2 = weather2.getString("main");
//
//                        String rawDate2=obj2.getString("dt_txt");
//                        rawDate2=rawDate2.substring(0,10);
//                        Integer weatherDate2 = (int) obj2.getDouble("dt");
//
//                        if (rawDate.equals(rawDate2)){
//                            if(maxTemp2>maxTemp){
//                                maxTemp=maxTemp2;
//                            }
//                            if(minTemp<minTemp2) {
//                                minTemp = minTemp2;
//                            }
//                        }
//                        else{
//                            Log.v("compare","rawDate: "+rawDate+ "   rawDate2: "+rawDate2);
//                            DailyWeatherReport report = new DailyWeatherReport(cityName,country,currentTemp.intValue(),maxTemp.intValue(),minTemp.intValue(),weatherType,rawDate,weatherDate);
//                            Log.v("JSON","Printing from class: "+report.getWeather()+ "   |time: "+rawDate);
//                            weatherReportList.add(report);
//
//                            obj = list.getJSONObject(x);
//                            main = obj.getJSONObject("main");
//                            currentTemp = main.getDouble("temp");
//                            maxTemp = main.getDouble("temp_max");
//                            minTemp = main.getDouble("temp_min");
//
//                            weatherArr = obj.getJSONArray("weather");
//                            weather = weatherArr.getJSONObject(0);
//                            weatherType = weather.getString("main");
//
//                            rawDate=obj.getString("dt_txt");
//                            rawDate=rawDate.substring(0,10);
//                            weatherDate = (int) obj.getDouble("dt");
//                        }
                        DailyWeatherReport report = new DailyWeatherReport(cityName,country,currentTemp.intValue(),maxTemp.intValue(),minTemp.intValue(),weatherType,rawDate,weatherDate);
                        Log.v("JSON","Printing from class: "+report.getWeather()+ "   |time: "+rawDate);
                        weatherReportList.add(report);
                    }

                    Log.v("JSON","Name"+cityName+" - "+"Country: " + country);
                } catch (JSONException e){
                    Log.v("JSON","EXC: " + e.getLocalizedMessage());
                }

                updateUI();
                mAdapter.notifyDataSetChanged();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("FUN","Err: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);

    }

    // this function is ued to update the main data so it changed when clicked.
    public void updateUI(){
        if (weatherReportList.size()>0){
            DailyWeatherReport report = weatherReportList.get(0);

            switch (report.getWeather()){
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.good));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.good));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.heavy_rain));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.heavy_rain));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_WIND:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.light_rain));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.light_rain));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snowy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.snowy));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }
            weatherDate.setText(report.getFormattedDate());
            rawDate.setText(report.getRawDate());
            currentTemp.setText(Integer.toString(report.getCurrentTemp()));
            lowTemp.setText(Integer.toString(report.getMinTemp()));
            cityCountry.setText(report.getCityName()+ ", "+report.getCountry());
            weatherDescription.setText(report.getWeather());
        }
    }

    // this function is used to run the download weather data from given location
    @Override
    public void onLocationChanged(Location location) {
        downloadWeatherData(location);
    }

    //this function prompt user for connection if they haven't done it before.
    @Override
    public void onConnected(@Nullable Bundle bundle){
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_LOCATION);
        } else{
            startLocationServices();
        }
    }

    //this function run when the connection failed , this function is empty for now, you can prompt an error message.
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){

    }

    //this function run when the connection suspended , this function is empty for now, you can prompt an error message.
    @Override
    public void onConnectionSuspended(int i){

    }

    //this function is ued to get location with GPS google
    public void startLocationServices(){
        try{
            LocationRequest req= LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);

/*            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null
                    if (location!=null){
                        //logic to handle location object
                    }
                }
            });*/
        } catch(SecurityException exception){

        }
    }

    //this function is used to get permission from the user for getting their location.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permission,grantResults);
        switch (requestCode){
            case PERMISSION_LOCATION:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startLocationServices();
                } else {
                    //show a dialog saying something like, "I can't run your dummy location"
                    Toast.makeText(this,"I can't run your location dummy - you denied permission!",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //This function is used to update the recycle/card view
    public class WeatherAdapter extends RecyclerView.Adapter<WeatherReportViewHolder>{

        private ArrayList<DailyWeatherReport> mDailyWeatherReports;

        public WeatherAdapter(ArrayList<DailyWeatherReport>dailyWeatherReports){
            mDailyWeatherReports = dailyWeatherReports;
        }

        @Override
        public void onBindViewHolder(WeatherReportViewHolder holder, int position){
            DailyWeatherReport report = mDailyWeatherReports.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount(){
            return mDailyWeatherReports.size();
        }

        @Override
        public WeatherReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather,parent,false);
            return new WeatherReportViewHolder(card);
        }
    }

    //This function is used to update the recycle/card view
    public class WeatherReportViewHolder extends RecyclerView.ViewHolder{

        private ImageView lweatherIcon;
        private TextView lweatherDate;
        private TextView lweatherDescription;
        private TextView ltempHigh;
        private TextView ltempLow;

        public WeatherReportViewHolder (View itemView) {
            super(itemView);

            lweatherIcon = (ImageView)itemView.findViewById(R.id.weather_icon);
            lweatherDate = (TextView)itemView.findViewById(R.id.weather_day);
            lweatherDescription = (TextView)itemView.findViewById(R.id.weather_description);
            ltempHigh = (TextView)itemView.findViewById(R.id.weather_temp_high);
            ltempLow = (TextView)itemView.findViewById(R.id.weather_temp_low);

        }

        public void updateUI(DailyWeatherReport report){

//            lweatherDate.setText(report.getFormattedDate());
            lweatherDate.setText(report.getRawDate());
            lweatherDescription.setText(report.getWeather());
            ltempHigh.setText(Integer.toString(report.getMaxTemp()));
            ltempLow.setText(Integer.toString(report.getMinTemp()));


            switch (report.getWeather()){
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.good));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.heavy_rain));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_WIND:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.light_rain));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snowy));
                    break;
                default:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }
        }

    }

}


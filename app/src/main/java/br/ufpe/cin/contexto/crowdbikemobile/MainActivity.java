package br.ufpe.cin.contexto.crowdbikemobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crowdbikemobile.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.ufpe.cin.br.adapter.crowdbikemobile.AdapterOcurrence;
import br.ufpe.cin.br.adapter.crowdbikemobile.Attributes;
import br.ufpe.cin.br.adapter.crowdbikemobile.Entity;
import br.ufpe.cin.br.adapter.crowdbikemobile.Ocorrencia;
import br.ufpe.cin.contexto.crowdbikemobile.async.AsyncSendNotification;
import br.ufpe.cin.contexto.crowdbikemobile.async.AsyncTempo;
import br.ufpe.cin.contexto.crowdbikemobile.pojo.Tempo;


@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback,
        LocationListener {
    private GoogleMap googleMap;
	private Point p;
	private String latitudeString = "";
	private String longitudeString = "";
	private Tempo tempoLocal = new Tempo();
	private int bgColor = 0;
	private TextView txtMensagem;
	private String IMEI;
	private AsyncSendNotification task2;
	private AsyncTempo tempo;
	private TextView txtResultado;
	private long timePosition;
	private boolean isGPSEnabled = true;
	private String lastLatitudeString;
	private String lastLongitudeString;
	private long timeLastPosition;
	private long anterior;
	private long atual;
	private boolean first = true;   //forecast


	private long anteriorForecast;
	private long atualForecast;
	private boolean firstForecast = true;

	private boolean firstLocation = true;
	private boolean doVoiceAlert;


	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final double EARTH_GRAVITY = 9.81;
	private static final double WEIGHT = 70.0;
	private static final double GRADE = 70.0;
	public static final double W_TO_KGM = 6.12;
	public static final double KGM_TO_KCAL = 1 / 427.0;

	private TextToSpeech TTS;
	// Lumped constant for all frictional losses (tires, bearings, chain).
	private static final double K1 = 0.0053;

	// Lumped constant for aerodynamic drag (kg/m)
	private static final double K2 = 0.185;

	private static double totalCalorias = 0.0;

	private DoSomethingThread randomWork;

	// The minimum distance to change Updates in meters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 1 meters
	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 0;// 1000 * 60 * 1; // 1
	// minute

	private static final String PREFS_REGISTERED = "Preferences";


	private String registered = "";
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
    private boolean threadsAlive = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


		txtMensagem = (TextView) findViewById(R.id.txtMensagem);
		IMEI = getIMEI(this);

		TTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status){
			}
		});

		Button btn_show = (Button) findViewById(R.id.btn_pop_up);
		btn_show.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//Open popup window
				if (p != null)
					pop_upMenu(MainActivity.this, p);
			}
		});

		txtResultado = (TextView) findViewById(R.id.txtResultado);

		// Setando a cor de fundo. Padrao: branco
		setarCorDeFundo(R.color.branco);

		callConnection();

		// Necess?rio para usar Runable na activity?
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		final Handler incomingMessageHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String message = msg.getData().getString("msg");
				TextView tv = (TextView) findViewById(R.id.txtMensagem);
				Date now = new Date();
				SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
				txtMensagem.setText(message);
				// tv.append(ft.format(now) + ' ' + message + '\n');
			}
		};

	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.view_map_action:
                displayMapActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Add a marker in Sydney, Australia, and move the camera.
        this.googleMap = map;

        //this.googleMap.setOnMapLongClickListener(this);
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setBuildingsEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

    }


    @Override
	public void onWindowFocusChanged(boolean hasFocus) {

		int[] location = new int[2];
		Button button = (Button) findViewById(R.id.btn_pop_up);

		// Get the x, y location and store it in the location[] array
		// location[0] = x, location[1] = y.
		button.getLocationOnScreen(location);

		//Initialize the Point with x, and y positions
		p = new Point();
		p.x = location[0];
		p.y = location[1];
	}

	public void pop_upMenu(final Activity context, Point p){
		int popupWidth = 300;
		int popupHeight = 280;

		// Inflate the popup_layout.xml
		LinearLayout viewGroup = (LinearLayout) context.findViewById(R.id.pop_up);
		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = layoutInflater.inflate(R.layout.pop_up, viewGroup);
		CheckBox voice_control = (CheckBox) layout.findViewById(R.id.VoiceAlert);
		voice_control.setChecked(doVoiceAlert);
		voice_control.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton group, boolean isChecked) {
				doVoiceAlert = isChecked;
			}
		});

		// Creating the PopupWindow
		final PopupWindow popup = new PopupWindow(context);
		popup.setContentView(layout);
		popup.setWidth(popupWidth);
		popup.setHeight(popupHeight);
		popup.setFocusable(true);

		// Some offset to align the popup a bit to the right, and a bit down, relative to button's position.
		int OFFSET_X = -20;
		int OFFSET_Y = 290;

		// Clear the default translucent background
		popup.setBackgroundDrawable(new BitmapDrawable());

		// Displaying the popup at the specified location, + offsets.
		popup.showAtLocation(layout, Gravity.NO_GRAVITY, p.x + OFFSET_X, p.y - OFFSET_Y);

	}

	@Override
	protected void onPause() {
		super.onPause();
		if(mGoogleApiClient != null){
			stopLocationUpdate();
		}
		threadsAlive = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(mGoogleApiClient !=null && mGoogleApiClient.isConnected()){
			startLocationUpdate();
		}
	}


	@Override
	protected void onStop(){
		super.onStop();
		threadsAlive = false;
	}

	@Override
	protected void onStart(){
		super.onStart();
		threadsAlive = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TTS.stop();
		TTS.shutdown();
	}


	public void setarCorDeFundo(int intColor) {
		setBgColor(intColor);
		//int color = R.color.yellow_smooth;
		String stringColor = getResources().getString(intColor);
		CardView cardView = (CardView) findViewById(R.id.cv);
		cardView.setCardBackgroundColor(Color.parseColor(stringColor));
	}

	public void displayMapActivity(){
		Intent intent = new Intent(this, MapDisplayActivity.class);
		startActivity(intent);
	}


	private void startGenerating() {
		threadsAlive = true;
		randomWork = new DoSomethingThread();
		randomWork.start();
	}

	public void updateResults(String resultado) throws Exception {
		retornoServidorFiware(resultado);
	}

	public class DoSomethingThread extends Thread {

		private static final String TAG = "DoSomethingThread";
		@Override
		public void run() {
			Log.v(TAG, "doing work in Random Number Thread");
			while (true) {
				try {
					publishProgress(fiwareRequest());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void publishProgress(String param) {
			Log.v(TAG, "reporting back from the consumer message Thread");
			final String resultado = param;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						updateResults(resultado);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}

	public String fiwareRequest() throws Exception {
		int responseCode = 0;
		String json = "";
        String result= "";
		String line = "";

		BufferedReader rd;
		try {
			String uri = "http://148.6.80.19:1026/v1/queryContext";
            String getAll = "{\"entities\": [{\"type\": \"Ocurrence\",\"isPattern\": \"true\",\"id\": \".*\"}],\"restriction\": " +
                    "{\"scopes\": [{\"type\" : \"FIWARE::Location\",\"value\" : {\"circle\": {\"centerLatitude\": \"" +
                    latitudeString +"\",\"centerLongitude\": \"" +longitudeString +"\",\"radius\": \"100\"}}}]}}";
			OkHttpClient client = new OkHttpClient();
			RequestBody body = RequestBody.create(JSON, getAll);
            Request request = new Request.Builder()
                    .url(uri)
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build();

            Response response;

            int executeCount = 0;
            do
            {
                response = client.newCall(request).execute();
                executeCount++;
            }
            while(response.code() == 408 && executeCount < 5);

            result = response.body().string();
            json = new JSONObject(result).toString();

		} catch (Exception e) {
			responseCode = 408;
			e.printStackTrace();
		}

		return json;
	}


/*
	public void tarefaParalelaServidor2() {
		// Instanciando a asynctask para contato com o servidor e acesso ao
		// arduino
		task2 = new AsyncSendNotification(this);
		task2.execute(IMEI, latitudeString, longitudeString);

	}
*/
	public void tarefaParalelaTempo() {
			// Instanciando a asynktask para contato com o servi?o de tempo
		if(firstForecast)
        {
            boolean findFirst = false;

            while(!findFirst) {

                tempo = new AsyncTempo(MainActivity.this);
                tempo.execute(latitudeString, longitudeString);
                try {
                    Tempo respostaTempo = tempo.get();
                    if(respostaTempo != null)
                    {
                        findFirst = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            firstForecast = false;
        }
        else
        {
            tempo = new AsyncTempo(MainActivity.this);
            tempo.execute(latitudeString, longitudeString);
        }
    }


	public void retornoServidorFiware(String retorno) throws Exception {
		Gson gson = new Gson();
		String noValues = "{\"errorCode\":{\"code\":\"404\",\"reasonPhrase\":\"No context element found\"}}";
		if (!retorno.equals(noValues) && !retorno.equals("")) {
			String distance = getDistanceLocation(retorno);
            Ocorrencia occ = getTipoOcorrencia(retorno);
			String title = occ.getTitle();
			if (distance != null && Double.valueOf(distance) <= 100) {

                setOccurenceCard(occ, distance);

				if (doVoiceAlert) {
					atual = System.nanoTime();
					if ((Double.parseDouble(distance) <= 100.0)) {
						if (first) {
							anterior = atual;
							if(threadsAlive) {
								notificacaoVoz(title, distance);
							}
							first = false;
						} else if (atual - anterior > 30000000000.0f) {
							if(threadsAlive) {
								notificacaoVoz(title, distance);
							}
							anterior = atual;
						}
					}
				}
			}
			Log.v("DIST", distance);
		} else {
			txtMensagem.setText("");
			setarCorDeFundo(R.color.branco);
		}

	}

    private void setOccurenceCard(Ocorrencia occ, String distance) {

        int occurenceTypeID = occ.getOccurenceCode();

        setarCorDeFundo(getOcurrenceColor(occurenceTypeID));

        ImageView iconWeather = (ImageView) findViewById(R.id.alert_img);
        iconWeather.setImageResource(getMarkViewTypeID(occurenceTypeID));

        TextView textView = (TextView) findViewById(R.id.txtMensagem);
        textView.setText(occ.getTitle()+": "+ (int)Double.parseDouble(distance)+"m");
        if(getOcurrenceColor(occurenceTypeID)==R.color.red_smooth){

            textView.setTextColor(ContextCompat.getColor(this, R.color.gray_white));

            TextView alertDetailsView = (TextView) findViewById(R.id.alert_details);
            alertDetailsView.setTextColor(ContextCompat.getColor(this, R.color.branco));
        }
//        v.height = h;
//        v.width = w;
//        iconWeather.setLayoutParams(v);
    }
    private int getOcurrenceColor(int occurenceTypeID){
        int markColor = R.color.branco;

        switch (occurenceTypeID){
            case 0:
                markColor = R.color.red_smooth;
                break;
            case 1:
                markColor = R.color.yellow_smooth;
                break;
            case 2:
                markColor = R.color.yellow_smooth;
                break;
            case 3:
                markColor = R.color.yellow_smooth;
                break;
        }
        return markColor;
    }

    private int getMarkViewTypeID(int occurenceTypeID){
        int markerViewTypeID = R.layout.mark_layout;

        switch (occurenceTypeID){
            case 0:
                markerViewTypeID = R.drawable.mark_accident_spot;
                break;
            case 1:
                markerViewTypeID = R.drawable.mark_heavy_traffic;
                break;
            case 2:
                markerViewTypeID = R.drawable.mark_bad_sinalization;
                break;
            case 3:
                markerViewTypeID = R.drawable.mark_rout_damaged;
                break;
        }
        return markerViewTypeID;
    }

    private void notificacaoVoz(String title, String distance){
        String mensagem = "Alerta: "+title + ((int)Double.parseDouble(distance))
                + "metros";
        //definir scopo de quando mandar a mensagem de voz, como identificar quando mandar.
        TTS.setPitch(1); // Afina??o da Voz
        TTS.setSpeechRate(1);//Velocidade da Voz
        TTS.speak(mensagem, TextToSpeech.QUEUE_FLUSH, null);
    }

	// xherman

	public String getDistanceLocation(String result) throws Exception {
		List<Entity> listEntity = AdapterOcurrence.parseListEntity(result);
		double minDistance = 0;
		double distance = 0;
		boolean isFirst = true;
		for (Entity entity : listEntity) {
			for (Attributes att : entity.getAttributes()) {
				if (att.getName().equalsIgnoreCase("GPSCoord")) {
					String[] tokensVal = att.getValue().split(",");
					distance = distance(tokensVal[0].trim(),
							tokensVal[1].trim(), latitudeString,
							longitudeString, 'M');
					if (isFirst) {
						minDistance = distance;
						isFirst = false;
					} else {
						if (minDistance < distance) {
							minDistance = distance;
						}
					}
				}
			}
		}
		return String.valueOf(minDistance);
	}

	public Ocorrencia getTipoOcorrencia(String result) throws Exception {
		List<Entity> listEntity = AdapterOcurrence.parseListEntity(result);
		boolean isFirst = true;
        Ocorrencia occ = new Ocorrencia();
		String title = "";
		for (Entity entity : listEntity) {
            occ = AdapterOcurrence.toOcurrence(entity);
		}
		return occ;
	}


	public void setTempoMain(Tempo tempoMain) {
		this.tempoLocal = tempoMain;

		if (tempoMain.getTemperatura() != null) {
			ImageView iconWeather = (ImageView) findViewById(R.id.iconWeather);
			TextView txtTemp = (TextView) findViewById(R.id.temperatura);
			TextView txtDesc = (TextView) findViewById(R.id.previsao);
//			TextView txtUom = (TextView) findViewById(R.id.txt_uom_temp);

			Integer temperatura = Double.valueOf(tempoLocal.getTemperatura())
					.intValue();

			// Exibindo o ?cone
			iconWeather.setImageResource(
                    tempoLocal.getIcone());

			// Exibindo a temperatura
			txtTemp.setText(temperatura.toString()+"\u2103");

			// Exibindo ˚C
			//txtUom.setText("˚C");

			// Exibindo a descricao
			txtDesc.setText(tempoLocal.getDescricao().trim());
		}
	}

	public String getLatitudeString() {
		return latitudeString;
	}

	public String getLongitudeString() {
		return longitudeString;
	}



	/**
	 * Este metodo recebe a resposta da chamada da ActivitySendNotification
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 30) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(getApplicationContext(), "Retorno",
						Toast.LENGTH_LONG).show();
				setarCorDeFundo(R.color.red_smooth);
			}
		}
	}

	public String getIMEI(Context context) {

		TelephonyManager mngr = (TelephonyManager) context
				.getSystemService(context.TELEPHONY_SERVICE);
		String imei = mngr.getDeviceId();
		return imei;

	}

	public int getBgColor() {
		return bgColor;
	}

	public void setBgColor(int bgColor) {
		this.bgColor = bgColor;
	}

	public double calcularCaloria() {


		double distance = distance(latitudeString, longitudeString,
				lastLatitudeString, lastLongitudeString, 'M');

		double durationSec = (timePosition - timeLastPosition) * (1 / 1000.0);
		double durationMin = durationSec * (1 / 1000.0) * (1 / 60.0);

		// Speed in m/s
		// double speed = (lastLocation.getSpeed() + newLocation.getSpeed()) /
		// 2.0;
		double speed = distance / durationSec;

		txtResultado.setText("Dist?ncia: " + distance + "\n Tempo: "
				+ durationSec + "s \n " + speed + "m/s");

		// Duration in min
		// double duration = (double) (newLocation.getTime() -
		// lastLocation.getTime()) * (1/1000.0) * (1/60.0);

		double power = EARTH_GRAVITY * WEIGHT * speed * (K1 + GRADE) + K2
				* (speed * speed * speed);

		// WorkRate in kgm/min
		double workRate = power * W_TO_KGM;

		// VO2 in kgm/min/kg 1.8 = oxygen cost of producing 1 kgm/min of power
		// output. 7 = oxygen cost of unloaded cycling plus resting oxygen
		// consumption
		double vo2 = (1.8 * workRate / WEIGHT) + 7;

		// Calorie in kcal
		totalCalorias = totalCalorias + vo2 * durationMin * WEIGHT
				* KGM_TO_KCAL;
		;

		// Toast.makeText(this, "calorias: " + totalCalorias,
		// Toast.LENGTH_LONG).show();

		// TextView txtCalorias = (TextView) findViewById(R.id.txtResultado);
		// txtCalorias.setText("Calorias:" + totalCalorias);
		txtResultado.setText(String.format("%.4f", totalCalorias) + " Cal");

		return totalCalorias;
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: Esse m?todo calcula a dist?ncia em K, M ou N : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private double distance(String latitude1, String longitude1,
			String latitude2, String longitude2, char unit) {

		double lat1 = Double.valueOf(latitude1).doubleValue();
		double lon1 = Double.valueOf(longitude1).doubleValue();
		double lat2 = Double.valueOf(latitude2).doubleValue();
		double lon2 = Double.valueOf(longitude2).doubleValue();
		double dist = 0.0;
		double R = 6372.8; // In kilometers

		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
				* Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.asin(Math.sqrt(a));

		dist = R * c;

		if (unit == 'K') {
			dist = dist * 1.609344;

		} else if (unit == 'N') {
			dist = dist * 0.8684;

		} else if (unit == 'M') {
			dist = dist * 1000.0;
		}
		return (dist);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */

	private double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}

	public void setLatitudeString(String latitudeString) {
		this.latitudeString = latitudeString;
	}

	public void setLongitudeString(String longitudeString) {
		this.longitudeString = longitudeString;
	}

	public void setTimePosition(long timePosition) {
		this.timePosition = timePosition;
	}

	public void setLastLatitudeString(String lastLatitudeString) {
		this.lastLatitudeString = lastLatitudeString;
	}

	public void setLastLongitudeString(String lastLongitudeString) {
		this.lastLongitudeString = lastLongitudeString;
	}

	public String getLastLatitudeString() {
		return lastLatitudeString;
	}

	public String getLastLongitudeString() {
		return lastLongitudeString;
	}

	public void setTimeLastPosition(long timeLastPosition) {
		this.timeLastPosition = timeLastPosition;
	}


	private synchronized void callConnection(){
		Log.i("LOG", "UpdateLocationActivity.callConnection()");
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addOnConnectionFailedListener(this)
				.addConnectionCallbacks(this)
				.addApi(LocationServices.API)
				.build();
		mGoogleApiClient.connect();
	}


	private void initLocationRequest(){
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(2000);
		mLocationRequest.setFastestInterval(1000);
		mLocationRequest.setSmallestDisplacement(1); // deslocamento mínimo em metros
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}


	private void startLocationUpdate(){
		initLocationRequest();
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
	}


	private void stopLocationUpdate(){
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,  this);
	}


	// LISTENERS
	@Override
	public void onConnected(Bundle bundle) {
		Log.i("LOG", "UpdateLocationActivity.onConnected(" + bundle + ")");

		Location mLastLocation = LocationServices
				.FusedLocationApi
				.getLastLocation(mGoogleApiClient); // PARA JÁ TER UMA COORDENADA PARA O UPDATE FEATURE UTILIZAR

        if(mLastLocation !=null){
            LatLng currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
        }

		startLocationUpdate();
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.i("LOG", "UpdateLocationActivity.onConnectionSuspended(" + i + ")");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.i("LOG", "UpdateLocationActivity.onConnectionFailed(" + connectionResult + ")");
	}


	@Override
	public void onLocationChanged(Location loc) {
		loc.getLatitude();
		loc.getLongitude();


			// Do something
			// Atualizando as informacoes do app
			setLatitudeString(String.valueOf(loc.getLatitude()));
			setLongitudeString(String.valueOf(loc.getLongitude()));
			if (firstLocation) {
				startGenerating();
				firstLocation = false;
			}


		// setando o momento da coordenada
		setTimePosition(System.currentTimeMillis());

		// calculandoa caloria do ultimo percurso
		if (getLastLatitudeString() != null) {
			if (getLastLongitudeString() != null) {
				//calcularCaloria();
			}
		}

		// setando a coordenada
		setLastLatitudeString(latitudeString);
		setLastLongitudeString(longitudeString);

		// setando o momento da coordenada
		setTimeLastPosition(timePosition);

        if(!latitudeString.equals("")&& !longitudeString.equals("")){
			atualForecast = System.nanoTime();
			if (firstForecast) {
				anteriorForecast = atualForecast;
				tarefaParalelaTempo();
				firstForecast = false;
				//forecast
			} else if (atualForecast - anteriorForecast > 60000000000.0f) {
				anteriorForecast = atualForecast;
				// forecast
				tarefaParalelaTempo();
			}
		}

	}




}

package ch.epfl.concertdrone.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import ch.epfl.concertdrone.R;
import ch.epfl.concertdrone.WearService;

//Copy of the AutonomusFLight to debug
public class DebugAutonomousFlightActivity extends AppCompatActivity {

    //Pour la comunication avec la montre
    public static final String DEBUG_ACTIVTY_SEND =
            "DEBUG_ACTIVTY_SEND";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_autonomous_flight);
        setContentView(R.layout.activity_debug_autonomous_flight);
    }


    public void onClickTryComunication(View view) {
        Toast.makeText(getApplicationContext(), "Sending", Toast.LENGTH_SHORT).show();
        sendMessagee("Ça marche enfin");//Envoi a la montre le string a linterieru
    }


    //Fontion pour envoyer un string a la montre
    public void sendMessagee(String mensaje) {
        Intent intent_send = new Intent(this, WearService.class);
        intent_send.setAction(WearService.ACTION_SEND.EXAMPLE_SEND_STRING_DUBUG.name());
        intent_send.putExtra(DEBUG_ACTIVTY_SEND, mensaje);
        startService(intent_send);
    }
}

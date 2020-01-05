package ch.epfl.concertdrone.activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatDialogFragment;
//For dialog of the initial activity
//Info source https://codinginflow.com/tutorials/android/simple-alertdialog

public class BlancDialog extends AppCompatDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Developers")
                .setMessage("This Drone Concert Photographer App was made by:\n- Bourban Emile\n- Guinchard Anthony\n- Houeix Acid Yann" +
                        "\n \nFor the course EE-490g – Lab On Apps Development For Tablets, Smartphones And Smartwatches, EPFL, Autumn 2019")
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        return builder.create();
    }
}
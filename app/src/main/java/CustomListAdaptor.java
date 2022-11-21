import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

public class CustomListAdaptor extends ArrayAdapter {
    //to reference the Activity
    private final Activity context = null;

    //to store the animal images
    private final Integer[] imageIDarray = {9999};


    //to store the list of countries
    private final String[] nameArray = {"8888"};

    //to store the list of countries
    private final String[] infoArray = {"7777"};

    public CustomListAdaptor(@NonNull Context context, int resource) {
        super(context, resource);


    }
}

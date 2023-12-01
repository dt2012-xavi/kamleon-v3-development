package com.dynatech2012.kamleonuserapp.repositories

import android.content.Context
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_DATEFIRESTORE
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_REALTIMEDAILY
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_REALTIMEMONTHLY
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil

object InternalStorage {
    //Firestore
    fun writeLastFirestoreDateOnInternalStorage(mcoContext: Context, sBody: String?) {
        //writeOnInternalStorage(mcoContext, "lastDateFirestoreChecked", sBody);
        SharedPrefUtil(mcoContext).saveString(PREF_DATEFIRESTORE, sBody)
    }

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * Throws not needed anymore
    </font> */
    fun readLastFirestoreDateOnInternalStorage(mcoContext: Context): String? {
        // throws  IOException {
        /*try {
            return readFromInternalStorage(mcoContext, "lastDateFirestoreChecked");
        } catch (IOException e) {
            return "null";
        }*/
        return SharedPrefUtil(mcoContext).getString(PREF_DATEFIRESTORE, null)
    }

    //Realtime Daily
    fun writeLastRealtimeDailyDateOnInternalStorage(mcoContext: Context, sBody: String?) {
        //writeOnInternalStorage(mcoContext, "lastDateRealtimeDailyChecked", sBody);
        SharedPrefUtil(mcoContext).saveString(PREF_REALTIMEDAILY, sBody)
    }

    fun readLastRealtimeDailyDateOnInternalStorage(mcoContext: Context): String? {
        /*try {
            return readFromInternalStorage(mcoContext, "lastDateRealtimeDailyChecked");
        } catch (IOException e) {
            return "null";
        }*/
        return SharedPrefUtil(mcoContext).getString(PREF_REALTIMEDAILY, null)
    }

    //Realtime Monthly
    fun writeLastRealtimeMonthlyDateOnInternalStorage(mcoContext: Context, sBody: String?) {
        //writeOnInternalStorage(mcoContext, "lastDateRealtimeMonthlyChecked", sBody);
        SharedPrefUtil(mcoContext).saveString(PREF_REALTIMEMONTHLY, sBody)
    }

    fun readLastRealtimeMonthlyDateOnInternalStorage(mcoContext: Context): String? {
        /*try {
            return readFromInternalStorage(mcoContext, "lastDateRealtimeMonthlyChecked");
        } catch (IOException e) {
            return "null";
        }*/
        return SharedPrefUtil(mcoContext).getString(PREF_REALTIMEMONTHLY, null)
    } /*//Write
    private static void writeOnInternalStorage(Context c, String filename, String string) {
        try {
            FileOutputStream fos = c.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(string.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Read
    private static String readFromInternalStorage(Context c, String filename) throws IOException {
        StringBuffer buffer = new StringBuffer();

        FileInputStream fis = c.openFileInput(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        String read;
        if (fis != null) {
            while ((read = reader.readLine()) != null) {
                buffer.append(read + "\n");
            }
        }
        fis.close();
        return buffer.toString();
    }*/
}
package com.emercy.canbus;

import com.emercy.canbus.Can.CanReceiver;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		byte[] data = {0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x78};
		
		Can can = new Can(60000);
		can.setLoopback(true).commit().start();
		can.receive(new Receiver());
//		can.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	class Receiver implements CanReceiver
	{

		@Override
		public void onCanReceive(int ID, int count, short[] data)
		{
			Log.d("MC","ID: "+ ID);
			Log.d("MC","count: "+ count);
			for(short s :data)
			Log.d("MC","data: "+ s+" ");
		}
		
	}
}

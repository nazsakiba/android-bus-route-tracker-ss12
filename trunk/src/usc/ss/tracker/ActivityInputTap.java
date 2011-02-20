package usc.ss.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ActivityInputTap extends ActivityBasic
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set layout content
		super.setContentView(R.layout.activity_input_tap);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_input_tap, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_tap_switch_grid:
				
				Intent intentGrid = new Intent(ActivityInputTap.this, ActivityInputGrid.class);
				startActivity(intentGrid);
				
				ActivityInputTap.this.finish();
				
				return true;
			case R.id.menu_tap_switch_gesture:
				
				Intent intentGesture = new Intent(ActivityInputTap.this, ActivityInputGesture.class);
				startActivity(intentGesture);
				
				ActivityInputTap.this.finish();
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
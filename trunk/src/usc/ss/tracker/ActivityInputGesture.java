package usc.ss.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ActivityInputGesture extends ActivityBasic
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set layout content
		super.setContentView(R.layout.activity_input_gesture);
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
			case R.id.menu_gesture_switch_grid:
				
				Intent intentGrid = new Intent(ActivityInputGesture.this, ActivityInputGrid.class);
				startActivity(intentGrid);
				
				ActivityInputGesture.this.finish();
				
				return true;
			case R.id.menu_gesture_switch_tap:
				
				Intent intentGesture = new Intent(ActivityInputGesture.this, ActivityInputTap.class);
				startActivity(intentGesture);
				
				ActivityInputGesture.this.finish();
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}

package usc.ss.tracker;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class ActivityInputGrid extends Activity
{
	protected static final String TAG = "ACTIVITY_INPUT_GRID";

	private TextToSpeech mTts;

	private ImageButton ibGrid;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set layout content
		setContentView(R.layout.activity_input_grid);

		ibGrid = (ImageButton) findViewById(R.id.linearlayout_grid);
		
		ibGrid.setOnTouchListener(new OnTouchListener()
		{	
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				Log.e(TAG, "Coordinates : " + event.getX() + " , " + event.getY());
				
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					Log.e(TAG, "Coordinates Down : " + event.getX() + " , " + event.getY());
				}
				
				if (event.getAction() == MotionEvent.ACTION_MOVE)
				{
					Log.e(TAG, "Coordinates : " + event.getX() + " , " + event.getY());
				}
				
				return false;
			}
		});
		
		/*button1 = (Button) findViewById(R.id.button_1);
		button2 = (Button) findViewById(R.id.button_2);

		
		button1.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					Log.e(TAG, "Test : " + button1.getText());
				}
				if (event.getAction() == MotionEvent.ACTION_UP)
				{
					Log.e(TAG, "Test : " + button1.getText());
				}

				return false;
			}
		});
		button2.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					Log.e(TAG, "Test : " + button2.getText());
				}
				if (event.getAction() == MotionEvent.ACTION_UP)
				{
					Log.e(TAG, "Test : " + button2.getText());
				}

				return false;
			}
		});

		button1.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Log.e(TAG, "Test Long Clicked: " + button1.getText());
				
				return false;
			}
		});
		button1.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.e(TAG, "Test Clicked: " + button1.getText());
			}
		});

		button2.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Log.e(TAG, "Test Long Clicked: " + button2.getText());
				
				return false;
			}
		});
		button2.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.e(TAG, "Test Clicked : " + button2.getText());
			}
		});*/

	}
}

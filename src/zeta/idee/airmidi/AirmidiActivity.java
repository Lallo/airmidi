package zeta.idee.airmidi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

public class AirmidiActivity extends Activity implements OnTouchListener {

	final String TAG = "AirmidiActivity";
	static MediaPlayer player;
	// -- ciao pippo
	String pollicino;
	Beat current_beat;
	boolean beatdetection = false;
	
	SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;
	float[] mGravity = null;
	float[] mGeomagnetic = null;
	SensorEventListener sensorListener;
	float bearing = 0.0f;
	static float azimutRadians = 0.0f;
	static float azimutDegree = 0.0f;
	static float indoorRadians = 0.0f;
	static float indoorDegree = 0.0f;
	float pitchDegree = 0.0f;
	float rollDegree = 0.0f;
	static int altitude = 0;
	static final int SENSOR_RATE = SensorManager.SENSOR_DELAY_UI;
	
	final static float PREDEFINED_ALTITUDE_ANGLE_NEUTRO = 5.0f;
	final static float PREDEFINED_ALTITUDE_ANGLE_BEAT = 5.0f;
	//final static int PREDEFINED_ALTITUDE_ANGLE_MAX = 30;
	final float PREDEFINED_DEGREE_TOLLERANCE = 2.0f;			// degree	
	
	int tones;
	int DEGREE_RANGE = 270;
	boolean isOn = true;
	int actualTone = -1;
	

    final int[] notes  = new int[] { 60, 62, 64, 65, 67, 69, 71, 72 };	
	final String[] scala = {"Do", "Re", "Mi", "Fa", "Sol", "La", "Si", "Do"};

	int[] instruments = new int[] {1, 10, 25, 48, 14, 12, 80, 57};
    int actualInstrument = instruments[0];

	
    float p_max = 0.0f;
    float p_last = 0.0f;
    float p_beat = 0.0f;
    
	String nome_del_file;
	String musicPath;
	
	CheckBox checkActive;
	Button buttonBeat;
	TextView toneView, lastBeat;
	ArrayAdapter<CharSequence> adapter;
	Spinner spinner;
	
	  // Note lengths
	  //  We are working with 32 ticks to the crotchet. So
	  //  all the other note lengths can be derived from this
	  //  basic figure. Note that the longest note we can
	  //  represent with this code is one tick short of a 
	  //  two semibreves (i.e., 8 crotchets)

	  public static final int SEMIQUAVER = 4;
	  public static final int QUAVER = 8;
	  public static final int CROTCHET = 16;
	  public static final int MINIM = 32;
	  public static final int SEMIBREVE = 64;

	  // Standard MIDI file header, for one-track file
	  // 4D, 54... are just magic numbers to identify the
	  //  headers
	  // Note that because we're only writing one track, we
	  //  can for simplicity combine the file and track headers
	  static final int header[] = new int[]
	     {
	     0x4d, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
	     0x00, 0x00, // single-track format
	     0x00, 0x01, // one track
	     0x00, 0x10, // 16 ticks per quarter
	     0x4d, 0x54, 0x72, 0x6B
	     };

	  // Standard footer
	  static final int footer[] = new int[]
	     {
	     0x01, 0xFF, 0x2F, 0x00
	     };

	  // A MIDI event to set the tempo
	  static final int tempoEvent[] = new int[]
	     {
	     0x00, 0xFF, 0x51, 0x03, 
	     0x0F, 0x42, 0x40 // Default 1 million usec per crotchet
	     };
	  
	  // A MIDI event to set the key signature. This is irrelent to
	  //  playback, but necessary for editing applications 
	  static final int keySigEvent[] = new int[]
	     {
	     0x00, 0xFF, 0x59, 0x02,
	     0x00, // C
	     0x00  // major
	     };
	  

	  // A MIDI event to set the time signature. This is irrelent to
	  //  playback, but necessary for editing applications 
	  static final int timeSigEvent[] = new int[]
	     {
	     0x00, 0xFF, 0x58, 0x04,
	     0x04, // numerator
	     0x02, // denominator (2==4, because it's a power of 2)
	     0x30, // ticks per click (not used)
	     0x08  // 32nd notes per crotchet 
	     };
	  
	  // The collection of events to play, in time order
	  protected Vector<int[]> playEvents;  
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
       // instruments = new int[] {1, 10, 25, 48, 115}; //r.getIntArray(R.array.instrumentsValuesArray);
        actualInstrument = instruments[0];
        
        //Log.i(TAG, "ai="+actualInstrument);
        toneView = (TextView) findViewById(R.id.toneView);
        toneView.setOnTouchListener(this);
        lastBeat = (TextView) findViewById(R.id.lastBeat);
        checkActive = (CheckBox) findViewById(R.id.checkActive);
        buttonBeat = (Button) findViewById(R.id.buttonBeat);
        buttonBeat.setOnTouchListener(this);
        
        spinner = (Spinner)this.findViewById(R.id.spinnerInstrument);
        adapter = ArrayAdapter.createFromResource(this, R.array.instrumentsStringArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
       
        tones = 8;
        
        // --> oggetti player
		player = new MediaPlayer();
		
		playEvents = new Vector<int[]>();
	    
		nome_del_file = "prova.mid";
		musicPath = Environment.getExternalStorageDirectory() + File.separator + "music" + File.separator;	
		Log.e(TAG, musicPath);
		
		//test();
		
		
		// --> oggetti sensori
		mGravity = null;
		mGeomagnetic = null;
 		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	    accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    sensorListener = new SensorEventListener(){
	    	  @Override
	    	  public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	    	  @Override
	    	  public void onSensorChanged(SensorEvent event) {
	    		  //Log.i(TAG, "onSensorChanged");
	    		  if(!checkActive.isChecked()) return;
	    		  if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) mGravity = event.values;
	    		  if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = event.values;	 	    		 
	    		  if(mGravity != null && mGeomagnetic != null) {
	    		        float R[] = new float[9];
	    		        float I[] = new float[9];
	    		        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
	    		        if (success) {
	    		          float orientation[] = new float[3];
	    		          SensorManager.getOrientation(R, orientation);
	    		          float newazr = orientation[0];
	    		          float newazd =  (float) (newazr * 180 / Math.PI);
	    		          Log.i(TAG, "newazd="+newazd);
	    		          boolean changed = false;
	    		          //if(Math.abs(azimutDegree - newazd) < PREDEFINED_DEGREE_TOLLERANCE) return;
	    		          pitchDegree = (float) (orientation[1] * 180 / Math.PI);
	    		          rollDegree = (float) (orientation[2] * 180 / Math.PI);
	    		          azimutRadians = newazr;
	    		          azimutDegree = newazd;
	    		          changed = true;
	    		          int tone = (int) ((azimutDegree+90) / (DEGREE_RANGE / tones));

	    		          int oldaltitude = altitude;

//	    		          if(beatdetection) { 
//	    		        	  if(current_beat.maxPitchDegree<pitchDegree) current_beat.maxPitchDegree=pitchDegree;
//	    		        	  if(current_beat.minPitchDegree>pitchDegree) current_beat.minPitchDegree=pitchDegree;
//	    		          }
	    		          
	    		          if(pitchDegree<p_last) {
	    		        	  // stai salendo
	    		        	 p_max = pitchDegree;
	    		          } else {
	    		        	  // stai scendendo
	    		          }
	    		          p_last = pitchDegree;
	    		          
	    		          if (pitchDegree < -PREDEFINED_ALTITUDE_ANGLE_NEUTRO) {
	    		                // top side up
	    		                //currentSide = Side.TOP;
	    		        	  	altitude = 1;
	    		          } else if (pitchDegree > PREDEFINED_ALTITUDE_ANGLE_BEAT) {
	    		                // bottom side up
	    		                //currentSide = Side.BOTTOM;
	    		        	    altitude = -1;
	    		        	    if(p_max<-PREDEFINED_ALTITUDE_ANGLE_NEUTRO) {
	    		        	    	// stai battendo il colpo
	    		        	    	if(actualTone!=-1 && true) playMe(p_max);
	    		        	    } else {
	    		        	    	// non avevi alzato abbastanza il cell
	    		        	    }
	    		        	    p_max = 0.0f;
	    		          } else if (pitchDegree >= -PREDEFINED_ALTITUDE_ANGLE_BEAT &&  pitchDegree <= PREDEFINED_ALTITUDE_ANGLE_NEUTRO) {
	    		        	  	altitude = 0;	    		        	 
	    		        	  if(tone<0 || tone >= tones) {
	    		        		  actualTone = -1;
	    		        		  toneView.setText("-");
	    		        	  } else {
	    		        		  if(actualTone!=tone) {
	    		        			  actualTone = tone;
	    		        			  toneView.setText(scala[actualTone]);
	    		        		  }
	    		        	  }
	    		          //} else if (pitchDegree>=(90+PREDEFINED_ALTITUDE_ANGLE_MAX) || pitchDegree<=-(90+PREDEFINED_ALTITUDE_ANGLE_MAX)) {
	    		          //	  	altitude = -2; // blocca la rilevazione: il device è troppo alzato o abbassato
	    		          } else if (rollDegree > 45) {
	    		                // right side up
	    		                //currentSide = Side.RIGHT;
	    		          } else if (rollDegree < -45) {
	    		                // left side up
	    		                //currentSide = Side.LEFT;
	    		          }
	    		          if(oldaltitude != altitude) changed = true;
	    		        }
	    		  }
	    	  } 
	      }; 
	      
	    Log.i(TAG, "inizializzati sensori");
	     
	    mSensorManager.registerListener(sensorListener, accelerometer, SENSOR_RATE);
        mSensorManager.registerListener(sensorListener, magnetometer, SENSOR_RATE);				
		
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //Object item = parent.getItemAtPosition(pos);
            	actualInstrument = instruments[pos];
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });       
        
	  }

    
    
    public void playMe(float h) {
    	
    	int height = (int) Math.abs(h);
    	lastBeat.setText(scala[actualTone]+" ("+height+")\n"+lastBeat.getText());
    	playEvents = new Vector<int[]>();
    	progChange (actualInstrument);
	    noteOnOffNow (SEMIQUAVER, notes[actualTone], height*2>127 ? 127 : height*2);
	    try {
			writeToFile(musicPath + nome_del_file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    playLocalSound(player, musicPath + nome_del_file, false);
	    
    }
 
    public void test() {
		
	    // Test 1 — play a C major chord
	    
	    // Turn on all three notes at start-of-track (delta=0) 
	    noteOn (0, 60, 127);
	    noteOn (0, 64, 127);
	    noteOn (0, 67, 127);

	    // Turn off all three notes after one minim. 
	    // NOTE delta value is cumulative — only _one_ of
	    //  these note-offs has a non-zero delta. The second and
	    //  third events are relative to the first
	    noteOff (MINIM, 60);
	    noteOff (0, 64);
	    noteOff (0, 67);

	    // Test 2 — play a scale using noteOnOffNow
	    //  We don't need any delta values here, so long as one
	    //  note comes straight after the previous one 
	    
	    progChange (73);

	    noteOnOffNow (QUAVER, 60, 127);
	    noteOnOffNow (QUAVER, 62, 127);
	    noteOnOffNow (QUAVER, 64, 127);
	    noteOnOffNow (QUAVER, 65, 127);
	    noteOnOffNow (QUAVER, 67, 127);
	    noteOnOffNow (QUAVER, 69, 127);
	    noteOnOffNow (QUAVER, 71, 127);
	    noteOnOffNow (QUAVER, 72, 127);

	    // Test 3 — play a short tune using noteSequenceFixedVelocity
	    //  Note the rest inserted with a note value of -1

	    int[] sequence = new int[]
	      {
	      60, QUAVER + SEMIQUAVER,
	      65, SEMIQUAVER,
	      70, CROTCHET + QUAVER,
	      69, QUAVER,
	      65, QUAVER / 3,
	      62, QUAVER / 3,
	      67, QUAVER / 3,
	      72, MINIM + QUAVER,
	      -1, SEMIQUAVER,
	      72, SEMIQUAVER,
	      76, MINIM,
	      };

	    // What the heck — use a different instrument for a change
	    progChange (41);

	    noteSequenceFixedVelocity (sequence, 127);	    
	    
	    playLocalSound(player, musicPath + nome_del_file, false);
	    
    	
    }
    
    /** Write the stored MIDI events to a file */
    public void writeToFile (String filename) throws IOException {
      
      //FileOutputStream fos =  openFileOutput(filename, Context.MODE_WORLD_READABLE);
      FileOutputStream fos = new FileOutputStream (filename);
      fos.write (intArrayToByteArray (header));

      // Calculate the amount of track data
      // _Do_ include the footer but _do not_ include the 
      // track header

      int size = tempoEvent.length + keySigEvent.length + timeSigEvent.length
        + footer.length;

      for (int i = 0; i < playEvents.size(); i++)
        size += playEvents.elementAt(i).length;

      // Write out the track data size in big-endian format
      // Note that this math is only valid for up to 64k of data
      //  (but that's a lot of notes) 
      int high = size / 256;
      int low = size - (high * 256);
      fos.write ((byte) 0);
      fos.write ((byte) 0);
      fos.write ((byte) high);
      fos.write ((byte) low);

    
      // Write the standard metadata — tempo, etc
      // At present, tempo is stuck at crotchet=60 
      fos.write (intArrayToByteArray (tempoEvent));
      fos.write (intArrayToByteArray (keySigEvent));
      fos.write (intArrayToByteArray (timeSigEvent));

      // Write out the note, etc., events
      for (int i = 0; i < playEvents.size(); i++)
      {
        fos.write (intArrayToByteArray (playEvents.elementAt(i)));
      }

      // Write the footer and close
      fos.write (intArrayToByteArray (footer));
      fos.flush();
      fos.close();
      
    }


    /** Convert an array of integers which are assumed to contain
        unsigned bytes into an array of bytes */ 
    protected static byte[] intArrayToByteArray (int[] ints)
    {
      int l = ints.length;
      byte[] out = new byte[ints.length]; 
      for (int i = 0; i < l; i++) 
      {
        out[i] = (byte) ints[i];
      }
      return out;
    }


    /** Store a note-on event */
    public void noteOn (int delta, int note, int velocity)
    {
    int[] data = new int[4];
    data[0] = delta;
    data[1] = 0x90;
    data[2] = note;
    data[3] = velocity;
    playEvents.add (data);
    }


    /** Store a note-off event */
    public void noteOff (int delta, int note)
    {
    int[] data = new int[4];
    data[0] = delta;
    data[1] = 0x80;
    data[2] = note;
    data[3] = 0;
    playEvents.add (data);
    }


    /** Store a program-change event at current position */
    public void progChange (int prog)
    {
    int[] data = new int[3];
    data[0] = 0;
    data[1] = 0xC0;
    data[2] = prog;
    playEvents.add (data);
    }


    /** Store a note-on event followed by a note-off event a note length
        later. There is no delta value — the note is assumed to
        follow the previous one with no gap. */
    public void noteOnOffNow (int duration, int note, int velocity)
    {
    noteOn (0, note, velocity);
    noteOff (duration, note);
    }

    
    public void noteSequenceFixedVelocity (int[] sequence, int velocity)
    {
      boolean lastWasRest = false;
      int restDelta = 0;
      for (int i = 0; i < sequence.length; i += 2)
      {
        int note = sequence[i];
        int duration = sequence[i + 1];
        if (note < 0) 
        {
          // This is a rest
          restDelta += duration;
          lastWasRest = true;
        } 
        else
        {
          // A note, not a rest
          if (lastWasRest)
          {
            noteOn (restDelta, note, velocity);
            noteOff (duration, note);
          }
          else
          {
            noteOn (0, note, velocity);
            noteOff (duration, note);
          }
          restDelta = 0;
          lastWasRest = false;
        }
      }
    }
    
    
    
    public void stopPlayer(MediaPlayer p) {
    	if(p.isPlaying()) p.stop();
    }
    
    
    public void playLocalSound(MediaPlayer p, String audioSource, boolean mustLoop) {
    	p.reset();
    	try {
			//File file = new File(getFilesDir() + "/" +audioSource);
			File file = new File(audioSource);
			Log.i(TAG, "nome:"+file.toString());
			if(!file.exists()) {
				Log.i(TAG, "file inesistente");
				return;
			}
			p.setDataSource(audioSource);
			Log.i(TAG, "caricato");
			p.prepare();
			//p.setLooping(mustLoop);
			p.start();
		} catch (Exception e) {
			Log.e(TAG,"playLocalSound: "+e.toString());
		}
    }
    
    public void playAssetSound(MediaPlayer p, String audioSource, boolean mustLoop) {
		try {
			p.reset();
			if(audioSource.length()==0) {
				Log.e("playEffect()", "silenced");
				return;
			}
			AssetFileDescriptor afd = getAssets().openFd(audioSource);
			p.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
			afd.close();
			p.prepare();
			p.setLooping(mustLoop);
			p.start();		
		} catch (Exception e) {
			Log.e("playAssetSound()", audioSource + "," + e.toString());
		}
    }


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.e(TAG, "id="+v.getId());
		if(v.getId()==buttonBeat.getId())  {
			if(checkActive.isChecked()) {
		       if(event.getAction() == MotionEvent.ACTION_DOWN) {
		            clickBeat();
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		            releaseBeat();
		        }
			}
		} else if(v.getId()==toneView.getId())  {
			if(checkActive.isChecked()) {
			       if(event.getAction() == MotionEvent.ACTION_DOWN) {
			             if(actualTone!=-1) playMe(100);
			        } else if (event.getAction() == MotionEvent.ACTION_UP) {
			        	//
			        }
				}
		}
		return false;
	}



	private void clickBeat() {
		beatdetection = true;
		current_beat = new Beat();
		current_beat.startTime = System.currentTimeMillis();
		current_beat.startAzimutDegree = azimutDegree;
		current_beat.startPitchDegree = pitchDegree;
		Log.d("startdetectBeat()", "startTime="+current_beat.startTime);				
	}	
	
	private void releaseBeat() {
		beatdetection = false;
		current_beat.endTime = System.currentTimeMillis();
		current_beat.endAzimutDegree = azimutDegree;
		current_beat.endPitchDegree = pitchDegree;
		Log.e("beat", "T="+(current_beat.endTime - current_beat.startTime)+",pRange=("+current_beat.minPitchDegree+","+current_beat.maxPitchDegree+"),aRange="+Math.abs(current_beat.startAzimutDegree - current_beat.endAzimutDegree));		
		//Log.e("beat", "T=" + (1.0 / ((current_beat.endTime - current_beat.startTime) / 1000)));
		// condizioni minime provvisorie
		if(current_beat.minPitchDegree<0 && current_beat.maxPitchDegree>0) {
			if(current_beat.startPitchDegree<0 && current_beat.endPitchDegree>0) {
				playMe(p_max);
			}
		}
	}
	
	public static class Beat {
		public double startAzimutDegree;
		public double endAzimutDegree;
		public double startPitchDegree;
		public double endPitchDegree;
		public double maxPitchDegree;
		public double minPitchDegree;
		public long startTime;
		public long endTime;
		public float distance;
		public Beat() {
			
		}
	}	
	
}
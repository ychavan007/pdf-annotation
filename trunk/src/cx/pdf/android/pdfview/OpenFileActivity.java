 package cx.pdf.android.pdfview;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.List;

import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import cx.pdf.android.pdfview.R;
import cx.pdf.android.lib.pagesview.FindResult;
import cx.pdf.android.lib.pagesview.PagesView;

/**
 * Document display activity.
 * PDF Annotation: 40% source code inserted
 * Date of last change: 20.04.2012
 */
public class OpenFileActivity extends Activity {
	
	private final static String TAG = "cx.hell.android.pdfview";
	
	private final static int[] zoomAnimations = {
		R.anim.zoom_disappear, R.anim.zoom_almost_disappear, R.anim.zoom
	};
	
	private final static int[] pageNumberAnimations = {
		R.anim.page_disappear, R.anim.page_almost_disappear, R.anim.page, 
		R.anim.page_show_always
	};
	
	private PDF pdf = null;
	private PagesView pagesView = null;
	private PDFPagesProvider pdfPagesProvider = null;
	private Actions actions = null;
	
	private Handler zoomHandler = null;
	private Handler pageHandler = null;
	private Runnable zoomRunnable = null;
	private Runnable pageRunnable = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem gotoPageMenuItem = null;
	private MenuItem rotateLeftMenuItem = null;
	private MenuItem rotateRightMenuItem = null;
	private MenuItem findTextMenuItem = null;
	private MenuItem clearFindTextMenuItem = null;
	private MenuItem chooseFileMenuItem = null;
	private MenuItem optionsMenuItem = null;
	private MenuItem saveAsMenuItem = null;
	private MenuItem annotListItem = null;
	
	private EditText pageNumberInputField = null;
	private EditText findTextInputField = null;
	
	private LinearLayout findButtonsLayout = null;
	private Button findPrevButton = null;
	private Button findNextButton = null;
	private Button findHideButton = null;
	
	private RelativeLayout activityLayout = null;
	private boolean eink = false;	

	// currently opened file path
	private String filePath = "/";
	
	private String findText = null;
	private Integer currentFindResultPage = null;
	private Integer currentFindResultNumber = null;

	// zoom buttons, layout and fade animation
	private ImageButton zoomDownButton;
	private ImageButton zoomWidthButton;
	private ImageButton zoomUpButton;
	private Animation zoomAnim;
	private LinearLayout zoomLayout;

	// page number display
	private TextView pageNumberTextView;
	private Animation pageNumberAnim;
	
	private int box = 2;

	private int fadeStartOffset = 7000; 
	
	public int annotSize = 20;
	
	private int colorMode = Options.COLOR_MODE_NORMAL;
	private static final int ZOOM_COLOR_NORMAL = 0;
	private static final int ZOOM_COLOR_RED = 1;
	private static final int ZOOM_COLOR_GREEN = 2;
	private static final int[] zoomUpId = {
		R.drawable.btn_zoom_up, R.drawable.red_btn_zoom_up, R.drawable.green_btn_zoom_up
	};
	private static final int[] zoomDownId = {
		R.drawable.btn_zoom_down, R.drawable.red_btn_zoom_down, R.drawable.green_btn_zoom_down		
	};
	private static final int[] zoomWidthId = {
		R.drawable.btn_zoom_width, R.drawable.red_btn_zoom_width, R.drawable.green_btn_zoom_width		
	};
	
	
    /**
     * Called when the activity is first created.
     * TODO: initialize dialog fast, then move file loading to other thread
     * TODO: add progress bar for file load
     * TODO: add progress icon for file rendering
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		Options.setOrientation(this);
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);
		
		
			

			


		
		this.box = Integer.parseInt(options.getString(Options.PREF_BOX, "2"));
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Get display metrics
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        // use a relative layout to stack the views
        activityLayout = new RelativeLayout(this);
        
        
        // the PDF view
        this.pagesView = new PagesView(this);
        activityLayout.addView(pagesView);
        startPDF(options);
        
        // the find buttons
        this.findButtonsLayout = new LinearLayout(this);
        this.findButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        this.findButtonsLayout.setVisibility(View.GONE);
        this.findButtonsLayout.setGravity(Gravity.CENTER);
        this.findPrevButton = new Button(this);
        this.findPrevButton.setText("Prev");
        this.findButtonsLayout.addView(this.findPrevButton);
        this.findNextButton = new Button(this);
        this.findNextButton.setText("Next");
        this.findButtonsLayout.addView(this.findNextButton);
        this.findHideButton = new Button(this);
        this.findHideButton.setText("Hide");
        this.findButtonsLayout.addView(this.findHideButton);
        this.setFindButtonHandlers();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        activityLayout.addView(this.findButtonsLayout, lp);

        this.pageNumberTextView = new TextView(this);
        this.pageNumberTextView.setTextSize(8f*metrics.density);
        lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        activityLayout.addView(this.pageNumberTextView, lp);
        
		// display this
        this.setContentView(activityLayout);
        
        // go to last viewed page
//        gotoLastPage();
        
        // send keyboard events to this view
        pagesView.setFocusable(true);
        pagesView.setFocusableInTouchMode(true);

        this.zoomHandler = new Handler();
        this.pageHandler = new Handler();
        this.zoomRunnable = new Runnable() {
        	public void run() {
        		fadeZoom();
        	}
        };
        this.pageRunnable = new Runnable() {
        	public void run() {
        		fadePage();
        	}
        };

    }
   

	/** 
	 * Save the current page before exiting
	 */
	@Override
	protected void onPause() {
		saveLastPage();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Options.setOrientation(this);
		
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

		boolean eink = options.getBoolean(Options.PREF_EINK, false);
		this.pagesView.setEink(eink);
		if (eink)
    		this.setTheme(android.R.style.Theme_Light);
		this.pagesView.setNook2(options.getBoolean(Options.PREF_NOOK2, false));
        
		actions = new Actions(options);
		this.pagesView.setActions(actions);

		setZoomLayout(options);
		this.pagesView.setSideMargins(options.getBoolean(Options.PREF_SIDE_MARGINS, false));
		this.pagesView.setDoubleTap(Integer.parseInt(options.getString(Options.PREF_DOUBLE_TAP, 
				""+Options.DOUBLE_TAP_ZOOM_IN_OUT)));
		
		int newBox = Integer.parseInt(options.getString(Options.PREF_BOX, "2"));
		if (this.box != newBox) {
			saveLastPage();
			this.box = newBox;
	        startPDF(options);
	        this.pagesView.goToBookmark();
		}

        this.colorMode = Options.getColorMode(options);
        this.eink = options.getBoolean(Options.PREF_EINK, false);
        this.pageNumberTextView.setBackgroundColor(Options.getBackColor(colorMode));
        this.pageNumberTextView.setTextColor(Options.getForeColor(colorMode));
        this.pdfPagesProvider.setGray(Options.isGray(this.colorMode));
        this.pdfPagesProvider.setExtraCache(1024*1024*Options.getIntFromString(options, Options.PREF_EXTRA_CACHE, 0));
        this.pdfPagesProvider.setOmitImages(options.getBoolean(Options.PREF_OMIT_IMAGES, false));
		this.pagesView.setColorMode(this.colorMode);		
		
		this.pdfPagesProvider.setRenderAhead(options.getBoolean(Options.PREF_RENDER_AHEAD, true));
		this.pagesView.setVerticalScrollLock(options.getBoolean(Options.PREF_VERTICAL_SCROLL_LOCK, false));
		this.pagesView.invalidate();
		int zoomAnimNumber = Integer.parseInt(options.getString(Options.PREF_ZOOM_ANIMATION, "2"));
		
		if (zoomAnimNumber == Options.ZOOM_BUTTONS_DISABLED)
			zoomAnim = null;
		else 
			zoomAnim = AnimationUtils.loadAnimation(this,
				zoomAnimations[zoomAnimNumber]);		
		int pageNumberAnimNumber = Integer.parseInt(options.getString(Options.PREF_PAGE_ANIMATION, "3"));
		
		if (pageNumberAnimNumber == Options.PAGE_NUMBER_DISABLED)
			pageNumberAnim = null;
		else 
			pageNumberAnim = AnimationUtils.loadAnimation(this,
				pageNumberAnimations[pageNumberAnimNumber]);		

		fadeStartOffset = 1000 * Integer.parseInt(options.getString(Options.PREF_FADE_SPEED, "7"));
		
		if (options.getBoolean(Options.PREF_FULLSCREEN, false))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		else
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.pageNumberTextView.setVisibility(pageNumberAnim == null ? View.GONE : View.VISIBLE);
		this.zoomLayout.setVisibility(zoomAnim == null ? View.GONE : View.VISIBLE);
        
        showAnimated(true);
	}

    /**
     * Set handlers on findNextButton and findHideButton.
     */
    private void setFindButtonHandlers() {
    	this.findPrevButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findPrev();
			}
    	});
    	this.findNextButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findNext();
			}
    	});
    	this.findHideButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findHide();
			}
    	});
    }
    
    /**
     * Set handlers on zoom level buttons
     */
    private void setZoomButtonHandlers() {
    	this.pdf = this.getPDF(); 
    	
    	this.zoomDownButton.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				pagesView.doAction(actions.getAction(Actions.LONG_ZOOM_IN));
				return true;
			}
    	});
    	this.zoomDownButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.doAction(actions.getAction(Actions.ZOOM_IN));
			}
    	});
    	this.zoomWidthButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.zoomWidth();
			}
    	});
    	this.zoomWidthButton.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				pagesView.zoomFit();
				return true;
			}
    	});
    	this.zoomUpButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.doAction(actions.getAction(Actions.ZOOM_OUT));
			}
    	});
    	this.zoomUpButton.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				pagesView.doAction(actions.getAction(Actions.LONG_ZOOM_OUT));
				return true;
			}
    	});
    }

    private void startPDF(SharedPreferences options) {
	    this.pdf = this.getPDF();
	    this.colorMode = Options.getColorMode(options);
	    this.pdfPagesProvider = new PDFPagesProvider(this, pdf,        		
	    		Options.isGray(this.colorMode), 
	    		options.getBoolean(Options.PREF_OMIT_IMAGES, false),
	    		options.getBoolean(Options.PREF_RENDER_AHEAD, true));
	    pagesView.setPagesProvider(pdfPagesProvider);
	    Bookmark b = new Bookmark(this.getApplicationContext()).open();
	    pagesView.setStartBookmark(b, filePath);
	    pagesView.setFilePath(filePath);
	    b.close();
	    final Annotation datasource = new Annotation(this);
	    datasource.options = PreferenceManager.getDefaultSharedPreferences(this);
	    startAnnotation(); // read annotations from PDF file 
    }



    /**
     * Return PDF instance wrapping file referenced by Intent.
     * Currently reads all bytes to memory, in future local files
     * should be passed to native code and remote ones should
     * be downloaded to local tmp dir.
     * @return PDF instance
     */
    private PDF getPDF() {
        final Intent intent = getIntent();
		Uri uri = intent.getData();    	
		filePath = uri.getPath();
		if (uri.getScheme().equals("file")) {
			Recent recent = new Recent(this);
			recent.add(0, filePath);
			recent.commit();
			return new PDF(new File(filePath), this.box);
    	} else if (uri.getScheme().equals("content")) {
    		ContentResolver cr = this.getContentResolver();
    		FileDescriptor fileDescriptor;
			try {
				fileDescriptor = cr.openFileDescriptor(uri, "r").getFileDescriptor();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e); // TODO: handle errors
			}
    		return new PDF(fileDescriptor, this.box);
    	} else {
    		throw new RuntimeException("don't know how to get filename from " + uri);
    	}
    }
    
    /**
     * Handle menu.
     * @param menuItem selected menu item
     * @return true if menu item was handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	if (menuItem == this.aboutMenuItem) {
			Intent intent = new Intent();
			intent.setClass(this, AboutPDFViewActivity.class);
			this.startActivity(intent);
    		return true;
    	} else if (menuItem == this.gotoPageMenuItem) {
    		this.showGotoPageDialog();
    	} else if (menuItem == this.saveAsMenuItem) {
    		this.showSaveAsDialog(false);
    	} else if (menuItem == this.rotateLeftMenuItem) {
    		this.pagesView.rotate(-1);
    	} else if (menuItem == this.rotateRightMenuItem) {
    		this.pagesView.rotate(1);
    	} else if (menuItem == this.findTextMenuItem) {
    		this.showFindDialog();
    	} else if (menuItem == this.clearFindTextMenuItem) {
    		this.clearFind();
    	} else if (menuItem == this.chooseFileMenuItem) {
    		startActivity(new Intent(this, ChooseFileActivity.class));
    	} else if (menuItem == this.optionsMenuItem) {
    		startActivity(new Intent(this, Options.class));
    	} else if (menuItem == this.annotListItem) {
    		Intent intent = new Intent();
			intent.setClass(this, AnnotationListActivity.class);
			this.startActivity(intent);
    		return true;
		} 
    	return false;
    }
    
    /**
     * Intercept touch events to handle the zoom buttons animation
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
    	int action = event.getAction();
    	if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
    		showAnimated(true);
    	}
		return super.dispatchTouchEvent(event);    	
    };
    
    public boolean dispatchKeyEvent(KeyEvent event) {
    	int action = event.getAction();
    	if (action == KeyEvent.ACTION_UP || action == KeyEvent.ACTION_DOWN) {
    		if (!eink)
    			showAnimated(false);
    	}
		return super.dispatchKeyEvent(event);    	
    };
    
    private void showZoom() {
    	if (zoomAnim == null) {
    		zoomLayout.setVisibility(View.GONE);
    		return;
    	}
    	
    	zoomLayout.setVisibility(View.VISIBLE);
    	zoomLayout.clearAnimation();
    	zoomHandler.removeCallbacks(zoomRunnable);
    	zoomHandler.postDelayed(zoomRunnable, fadeStartOffset);
    }
    
    private void fadeZoom() {
    	if (eink || zoomAnim == null) {
    		zoomLayout.setVisibility(View.GONE);
    	}
    	else {
    		zoomAnim.setStartOffset(0);
    		zoomAnim.setFillAfter(true);
    		zoomLayout.startAnimation(zoomAnim);
    	}
    }
    
    public void showPageNumber(boolean force) {
    	if (pageNumberAnim == null) {
    		pageNumberTextView.setVisibility(View.GONE);
    		return;
    	}
    	
    	pageNumberTextView.setVisibility(View.VISIBLE);
    	String newText = ""+(this.pagesView.getCurrentPage()+1)+"/"+
				this.pdfPagesProvider.getPageCount();
    	
    	if (!force && newText.equals(pageNumberTextView.getText()))
    		return;
    	
		pageNumberTextView.setText(newText);
    	pageNumberTextView.clearAnimation();

    	pageHandler.removeCallbacks(pageRunnable);
    	pageHandler.postDelayed(pageRunnable, fadeStartOffset);
    }
    
    private void fadePage() {
    	if (eink || pageNumberAnim == null) {
    		pageNumberTextView.setVisibility(View.GONE);
    	}
    	else {
    		pageNumberAnim.setStartOffset(0);
    		pageNumberAnim.setFillAfter(true);
    		pageNumberTextView.startAnimation(pageNumberAnim);
    	}
    }    
    
    /**
     * Show zoom buttons and page number
     */
    private void showAnimated(boolean alsoZoom) {
    	if (alsoZoom)
    		showZoom();
    	showPageNumber(true);
    }
    
    /**
     * Hide the find buttons
     */
    private void clearFind() {
		this.currentFindResultPage = null;
		this.currentFindResultNumber = null;
    	this.pagesView.setFindMode(false);
		this.findButtonsLayout.setVisibility(View.GONE);
    }
    
    /**
     * Show error message to user.
     * @param message message to show
     */
    private void errorMessage(String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	AlertDialog dialog = builder.setMessage(message).setTitle("Error").create();
    	dialog.show();
    }
    
    /**
     * Called from menu when user want to go to specific page.
     */
    private void showGotoPageDialog() {
    	final Dialog d = new Dialog(this);
    	d.setTitle(R.string.goto_page_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	TextView label = new TextView(this);
    	final int pagecount = this.pdfPagesProvider.getPageCount();
    	label.setText("Page number from " + 1 + " to " + pagecount);
    	this.pageNumberInputField = new EditText(this);
    	this.pageNumberInputField.setInputType(InputType.TYPE_CLASS_NUMBER);
    	this.pageNumberInputField.setText("" + (this.pagesView.getCurrentPage() + 1));
    	Button goButton = new Button(this);
    	goButton.setText(R.string.goto_page_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int pageNumber = -1;
				try {
					pageNumber = Integer.parseInt(OpenFileActivity.this.pageNumberInputField.getText().toString())-1;
				} catch (NumberFormatException e) {
					/* ignore */
				}
				d.dismiss();
				if (pageNumber >= 0 && pageNumber < pagecount) {
					OpenFileActivity.this.gotoPage(pageNumber);

				} else {
					OpenFileActivity.this.errorMessage("Invalid page number");
				}
			}
    	});
    	Button page1Button = new Button(this);
    	page1Button.setText(getResources().getString(R.string.page) +" 1");
    	page1Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(0);
			}
    	});
    	Button lastPageButton = new Button(this);
    	lastPageButton.setText(getResources().getString(R.string.page) +" "+pagecount);
    	lastPageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(pagecount-1);
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(label, params);
    	contents.addView(pageNumberInputField, params);
    	contents.addView(goButton, params);
    	contents.addView(page1Button, params);
    	contents.addView(lastPageButton, params);
    	d.setContentView(contents);
    	d.show();
    }
    
    private void gotoPage(int page) {
    	Log.i(TAG, "rewind to page " + page);
    	if (this.pagesView != null) {
    		this.pagesView.scrollToPage(page);
            showAnimated(true);
    	}
    }
    
   /**
     * Save the last page in the bookmarks
     */
    private void saveLastPage() {
    	BookmarkEntry entry = this.pagesView.toBookmarkEntry();
        Bookmark b = new Bookmark(this.getApplicationContext()).open();
        b.setLast(filePath, entry);
        b.close();
        Log.i(TAG, "last page saved for "+filePath);    
    }
    
    /**
     * 
     * Create options menu, called by Android system.
     * @param menu menu to populate
     * @return true meaning that menu was populated
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	this.saveAsMenuItem = menu.add(R.string.save_as);
    	this.gotoPageMenuItem = menu.add(R.string.goto_page);
    	this.annotListItem = menu.add(R.string.annotationList);
    	//this.rotateRightMenuItem = menu.add(R.string.rotate_page_left);
    	//this.rotateLeftMenuItem = menu.add(R.string.rotate_page_right);
    	this.clearFindTextMenuItem = menu.add(R.string.clear_find_text);
    	this.chooseFileMenuItem = menu.add(R.string.choose_file);
    	this.optionsMenuItem = menu.add(R.string.options);
    	/* The following appear on the second page.  The find item can safely be kept
    	 * there since it can also be accessed from the search key on most devices.
    	 */
		this.findTextMenuItem = menu.add(R.string.find_text);
		this.rotateRightMenuItem = menu.add(R.string.rotate_page_left);
    	this.rotateLeftMenuItem = menu.add(R.string.rotate_page_right);
    	this.aboutMenuItem = menu.add(R.string.about);
    	return true;
    }
        
    /**
     * Prepare menu contents.
     * Hide or show "Clear find results" menu item depending on whether
     * we're in find mode.
     * @param menu menu that should be prepared
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	this.clearFindTextMenuItem.setVisible(this.pagesView.getFindMode());
    	return true;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      Log.i(TAG, "onConfigurationChanged(" + newConfig + ")");
    }
    
    /**
     * Show find dialog.
     * Very pretty UI code ;)
     */
    public void showFindDialog() {
    	Log.d(TAG, "find dialog...");
    	final Dialog dialog = new Dialog(this);
    	dialog.setTitle(R.string.find_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	this.findTextInputField = new EditText(this);
    	this.findTextInputField.setWidth(this.pagesView.getWidth() * 80 / 100);
    	Button goButton = new Button(this);
    	goButton.setText(R.string.find_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = OpenFileActivity.this.findTextInputField.getText().toString();
				OpenFileActivity.this.findText(text);
				dialog.dismiss();
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(findTextInputField, params);
    	contents.addView(goButton, params);
    	dialog.setContentView(contents);
    	dialog.show();
    }
    
    /***** START OF SAVE AS FUNCTIONALITY *****/
    
    /**
     * Confirm save annotation to PDF file
     * @param oldFile name of old file
     * @param newFile name of new file
     * @return
     */
    private boolean confirmedSaveToFile (File oldFile, File newFile) {
    	final Annotation datasource = new Annotation(this);
    	boolean confirm = false;
    	
    	try {
			datasource.open();
			// save annotation to PDF file
			confirm = datasource.saveAnnotToFile(oldFile, newFile);
			// actualize annotations
			if (confirm) {
				datasource.actualizeAnnots();
			}
			
	    } finally {
	    	if (datasource != null) {
	    		// close database connection
	    		datasource.close();
	    	}
	    }
    	return confirm;
    }

    
    /**
     * Show "save as" dialog 
     * @param exit exit after show dialog
     */
    public void showSaveAsDialog(final Boolean exit) {
    	
    	File file = new File(filePath);

    	final Dialog dialog = new Dialog(this);
    	// dialog title
    	dialog.setTitle(R.string.save_as);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	// file name
    	final EditText fileText = new EditText(this);
    	fileText.setText(file.getName().replace(".pdf", ""));
    	fileText.setWidth(this.pagesView.getWidth() * 80 / 100);
    	
    	// positive button
    	Button positiveButton = new Button(this);
    	positiveButton.setText(R.string.save);
    	positiveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				final File oldFile = new File(filePath);
				final File newFile = new File(oldFile.getParent() + "/" + fileText.getText().toString() + ".pdf");

				// new file exists, overwrite?
				if (newFile.exists()) {
					AlertDialog.Builder adbuilder = new AlertDialog.Builder(OpenFileActivity.this);
					adbuilder.setMessage(R.string.overwrite_file)
					       .setCancelable(true)
					       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface ddialog, int id) {
					        	   ddialog.dismiss(); // close overwrite dialog
					        	   dialog.dismiss(); // close file name dialog
					        	   // save document to specified file
					        	   if (confirmedSaveToFile(oldFile, newFile)) {
					        		   // exit opened file activity
					        		   if (exit == true) {
					        			   OpenFileActivity.this.finish();
					        		   } else {
					        			   // successfully saved
					        			   makeToast(R.string.file_saved);
					        		   } 
					        		
					        	   } else {
					        		   // save error!
					        		   makeToast(R.string.save_error);
					        	   }
					        	   
					           }
					       })
					       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					           }
					       });
					AlertDialog alertDialog = adbuilder.create();
					alertDialog.show();
					
				} else {
					confirmedSaveToFile(oldFile, newFile); 
					// exit opened file activity
			    	if (exit == true) {
			    		OpenFileActivity.this.finish();
			    	} else {
			    		// successfully saved
	        			makeToast(R.string.file_saved);
			    	}
					dialog.dismiss();
				}
			}
    	});
    	
    	// negative button
    	Button negativeButton = new Button(this);
    	negativeButton.setText(R.string.cancel);
    	negativeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
    			LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	
    	contents.addView(fileText, params);
    	contents.addView(positiveButton, params);
    	contents.addView(negativeButton, params);
    	dialog.setContentView(contents);
    	dialog.show();
    	
    }

    /***** START OF ANNOTATION ACTIVITY *****/
    
	/**
	 * Check changes annotations before exiting
	 */
	@Override
	public void onBackPressed() {
		checkAnnots();
	}
	
	/** Item of action list */
	private static class Item {
	    public final String text;
	    public final int icon;
	    public Item(String text, Integer icon) {
	        this.text = text;
	        this.icon = icon;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }
	    
	}
	
    /**
     * Show add new annotation dialog (add text, circle or square type of annotation).
     * @param x lower left x position
     * @param y lower left y position
     */
    public void showAnnotDialog(final float x, final float y, final int page) {  
    	new AlertDialog.Builder(this)
        .setTitle(R.string.add_annotation)
        .setIcon(android.R.drawable.ic_menu_more)
        .setAdapter(getEditActionList(getEditActionsText(4, "none")),
        		new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int item) {
    	    	annotationContext(page+1, item, x, y, -1);
    	    	dialog.dismiss();
    	    }
        	
        }).show();

    }
   
    
    /**
     * Show text of annotation (author, title, date of last modified and contents).
     * @param position
     * @param cursor
     */
    public void showAnnotText(final int position, final Cursor cursor) {
    	cursor.moveToPosition(position);

    	// alertDialog
    	LayoutInflater li = getLayoutInflater();
    	View view = li.inflate(R.layout.annot_message, null);
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.create();

    	// subject of annotation
    	TextView annot_subject = (TextView) view.findViewById(R.id.annot_subject);
    	String subject = cursor.getString(cursor.getColumnIndex("subject"));
    	if (subject != "unknown") {
    		annot_subject.setText(subject);
        } else {
        	annot_subject.setVisibility(View.GONE);
    	}
    	
    	// author of annotation
    	TextView annot_author = (TextView) view.findViewById(R.id.annot_author);
    	String author = cursor.getString(cursor.getColumnIndex("author"));
    	if (author != "unknown") {
    		annot_author.setText(author);
    	} else {
    		annot_author.setVisibility(View.GONE);
    	}
    	
    	// time of last modification
    	TextView annot_moddate = (TextView) view.findViewById(R.id.annot_moddate);
    	String moddate = cursor.getString(cursor.getColumnIndex("moddate"));
    	if (moddate != "unknown") {
    		annot_moddate.setText(convertDateFormat(moddate));
    	} else {
    		annot_moddate.setVisibility(View.GONE);
    	}
    	
    	// contents of annotation
    	TextView annot_text = (TextView) view.findViewById(R.id.annot_text);
    	annot_text.setText(cursor.getString(cursor.getColumnIndex("contents")));

    	// OPTIONS button
    	builder.setPositiveButton(R.string.options, new DialogInterface.OnClickListener() {
    		public void onClick(final DialogInterface dialog, int id) {

    			AlertDialog.Builder builderEdit = new AlertDialog.Builder(OpenFileActivity.this);
    			builderEdit.setTitle(R.string.choose_action);
    			builderEdit.setIcon(android.R.drawable.ic_menu_more);
                builderEdit.setAdapter(getEditActionList (getEditActionsText(
                	cursor.getInt(cursor.getColumnIndex("flag")), cursor.getString(cursor.getColumnIndex("subtype")))), 
                	new DialogInterface.OnClickListener() {
                   
                	public void onClick(DialogInterface dialogInterface, int item) {
                		changeAnnotation(cursor, position, item);
                    	dialogInterface.dismiss();
                    	dialog.cancel();
                    }
                });
                
                builderEdit.show();
			}
        });	
    	
    	// OK button, annotation exists at PDF document, position don't change
        builder.setNegativeButton(R.string.ann_ok, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int id) {
        		dialog.cancel();
        	}	
        });
    	
    	// show annotation dialog
    	builder.setView(view);
    	builder.show();
    	
    }
    
    /**
     * Control textual annotation.
     * @param subtype subtype of annotation
     * @return true if text subtype, false other way
     */
    private boolean isTextSubtype (String subtype) {
    	if (subtype.equalsIgnoreCase("Text")) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Convert PDF date-time format.
     * @param moddate PDF date
     * @return application converted date
     */
    private String convertDateFormat (String moddate) {
    	 final Annotation datasource = new Annotation(this);
    	 return datasource.convertDateFormat(moddate);
    }
    
    /**
     * Create menu (add new annotations, edit annotations).
     * @param flag flag of annotation (add, edit etc.)
     * @param subtype subtype of annotation (text, circle or square)
     * @return array of menu items
     */
    private Item[] getEditActionsText (int flag, String subtype) {
    	Resources res = getResources();
    	// new annotation menu (text, circle or square subtype)
    	if (flag == 4) {
    		Item[] items = {
    			new Item(String.format(res.getString(R.string.text_annotation)), 
    	    		android.R.drawable.ic_menu_add),
    	    	new Item(String.format(res.getString(R.string.circle_annotation)), 
    	    		android.R.drawable.ic_menu_add),
    	    	new Item(String.format(res.getString(R.string.square_annotation)), 
    	    		android.R.drawable.ic_menu_add)
    	    	};
    		return items;
    		
    	// edit annotation menu
    	} else {
    		// edit
    		Item ann_edit = new Item(String.format(res.getString(R.string.ann_edit)), 
					R.drawable.ic_menu_edit);
    		// delete
    		Item ann_delete = new Item(String.format(res.getString(R.string.ann_delete)), 
					R.drawable.ic_menu_delete);
    		// move
    		Item ann_move = new Item(String.format(res.getString(R.string.ann_move)), 
    				R.drawable.ic_menu_move);
    		// resize
    		Item ann_resize = new Item(String.format(res.getString(R.string.ann_resize)), 
					R.drawable.ic_menu_resize);
    		
    		// circle or square annotation
    		if (!isTextSubtype(subtype)) {
    			Item[] items = {ann_edit, ann_delete, ann_move, ann_resize};
    			return items;
    		} else {
    			Item[] items = {ann_edit, ann_delete, ann_move};
    			return items;
    		}    	
    	} 
    }
    
    private ListAdapter getEditActionList (final Item[] items) {
    	ListAdapter listAdapter = new ArrayAdapter<Item> (this, android.R.layout.select_dialog_item,
    		android.R.id.text1, items) {
    		
    		public View getView(int position, View convertView, ViewGroup parent) {
    			// user super class to create the View
    		    View view = super.getView(position, convertView, parent);
    		    TextView textView = (TextView) view.findViewById(android.R.id.text1);

    		    // put the image on the TextView
    		    textView.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

    			// add margin between image and text
    			int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.4f);
    			textView.setCompoundDrawablePadding(dp5);

    			return view;
    		}
    	};
    	
		return listAdapter;
    }
    
    /**
     * Action with actual annotation 
     * @param cursor database array of actual item
     * @param action (edit, delete, change position or change size)
     */
    private void changeAnnotation (Cursor cursor, int position, int action) {
    	cursor.moveToPosition(position);
    	switch (action) {
    	case 0 : // edit (text, color, type of annotation)
    		editSelectedAnnotation(cursor);
    		break;
    	case 1 : // delete 
    		deleteSelectedAnnotation(cursor);
    		break;
    	case 2 : // change position
    		OpenFileActivity.this.pagesView.setAnnotMoveSizeMode(cursor.getInt(cursor.getColumnIndex("_id")), 0);
    		break;
    	case 3 : // change size
    		OpenFileActivity.this.pagesView.setAnnotMoveSizeMode(cursor.getInt(cursor.getColumnIndex("_id")), 1);
    		break;
    	
    	}
    }
    
    /**
     * Edit selected annotation
     * @param cursor database cursor
     */
    private void editSelectedAnnotation (Cursor cursor) {
    	int actPage = (this.pagesView.getCurrentPage()+1);
    	try {
			annotationContext(actPage, 1, 
				cursor.getFloat(cursor.getColumnIndex("llx")), 
				cursor.getFloat(cursor.getColumnIndex("lly")),
				cursor.getInt(cursor.getColumnIndex("_id")));
		} catch (IllegalStateException e) {
			Log.e(TAG, "Illegal State Exception: " + e);
		}
    }
    
    /**
     * Make toast text information
     * @param text resource string identifier
     */
    public void makeToast (int text) {
    	Toast.makeText(getApplicationContext(), text, 
    	Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Make vibrate 
     * @param ms time at ms
     */
    public void makeVibrate (int ms) {
    	// Get instance of Vibrator from current Context
		Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		 
		v.vibrate(ms);
    }
    
    /**
     * Delete selected annotation
     * @param cursor database cursor
     */
    private void deleteSelectedAnnotation (final Cursor cursor) {
    	final Annotation datasource = new Annotation(this);
		try {
       		AlertDialog.Builder builder = new AlertDialog.Builder(OpenFileActivity.this);
    		builder.setMessage(R.string.delete_annotation_question)
    		       .setCancelable(true)
    		       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		        	   dialog.cancel();
    		        		// delete annotation from database
    		          		datasource.open();
    		          		datasource.deleteAnnotation(cursor.getInt(cursor.getColumnIndex("_id")));
    		          		
    		          		// show toast
    		          		makeToast(R.string.annotation_deleted);
    		          		
    		          		OpenFileActivity.this.pagesView.invalidate();
    		           }
    		       })
    		       .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		                dialog.cancel();
    		           }
    		       });

    		builder.create().show();
       	} finally {
       	   	// close database connection
       	   	if (datasource != null) {
       	   		datasource.close();
       	   	}
       	   		
       	}
        
		OpenFileActivity.this.pagesView.invalidate();
    }
    /**
     * Save new position or size of annotation to SQLite database
     * @param id identifier of annotation
     * @param posX X position
     * @param posY Y position
     * @param action if 0 update position, if 1 update size
     */
    public void saveNewAnnotPos (int id, float posX, float posY, int action) {
    	final Annotation datasource = new Annotation(this);
	    try {
			datasource.open();	
			datasource.saveNewAnnotPos(id, posX, posY, action);
	    } finally {
	    	if (datasource != null) {
	    		// close database connection
	    		datasource.close();
	    	}
	    }
    }
    
    /**
     * Load annotations from PDF file to a database.
     */
    private void startAnnotation () {
	    final Annotation datasource = new Annotation(this);
	    int actPage = (this.pagesView.getCurrentPage()+1);
	    try {
			datasource.open();	
			// truncate annotations
			datasource.truncateAnnots();
			// insert annotations from PDF file to database
			datasource.loadAnnotFromFile(filePath, actPage);
	    } catch (Exception e) {
	    	Log.w(TAG, "erase database annot.db: " + e);
	    } finally {
	    	if (datasource != null) {
	    		// close database connection
	    		datasource.close();
	    	}
	    }
    }
    
    /**
     * Show annotation context activity.
     * @param page actual page number
     * @param index subtype of annotation (text(1), circle(2), square(3))
     * @param x position
     * @param y position
     * @param id identifier of annotation
     */
    private void annotationContext(int page, int index, float x, float y, int id) {
    	Intent intent = new Intent();
		intent.setClass(this, AnnotationActivity.class);
		intent.putExtra("llx", x);
		intent.putExtra("lly", y);
		intent.putExtra("id", id);
		intent.putExtra("subtype", index);
		intent.putExtra("page", page);
		this.startActivity(intent);
    }
    
    /**
     * Get annotations from SQLite database.
     * @param page page number
     */
    public Cursor getAnnotsFromSQL(int page, int... params) {
    	assert params.length <= 1;
	    boolean app = params.length > 0 ? true : false;
    	Annotation datasource = new Annotation(this);
    	Cursor cursor = null;
    	try {
			datasource.open();
			if (app)
				cursor = datasource.getFileAnnots(page, params[0]);
			else
				cursor = datasource.getFileAnnots(page);
			
	    } finally {
	    	// close database connection
	    	if (datasource != null) {
	    		datasource.close();
	    	}
	    }

    	return cursor;
    }
    
    /**
     * Get annotation reference by identifier.
     * @param id identifier of annotation
     * @return database cursor
     */
    public Cursor getAnnotById(int id) {
    	Annotation datasource = new Annotation(this);
    	Cursor cursor = null;
    	try {
    		// add new annotation entry to database
			datasource.open();
			cursor = datasource.getAnnotById(id);
			
	    } finally {
	    	// close database connection
	    	if (datasource != null) {
	    		datasource.close();
	    	}
	    }

    	return cursor;
    }
    
    /**
     * Show dialog: Save annotations before exit?
     */
    private void checkAnnots () {
    	Annotation datasource = new Annotation(this);

    	if (datasource.checkBeforeExit()) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(OpenFileActivity.this);
    		builder.setMessage(R.string.save_annotation_before_exit)
    		       .setCancelable(true)
    		       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		        	   dialog.cancel();
    		        	   showSaveAsDialog(true);
    		           }
    		       })
    		       .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		                dialog.cancel();
    		                OpenFileActivity.this.finish();
    		           }
    		       })
		    		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				       public void onClick(DialogInterface dialog, int id) {
				            dialog.cancel();
				       }
				   });
    		builder.create().show();
    	} else {
    		OpenFileActivity.this.finish();
    	}
    	return;
    }
    
    public String getFileName () {
    	return this.filePath;
    }
    
    /***** END OF ANNOTATION ACTIVITY *****/
    
    
    private void setZoomLayout(SharedPreferences options) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        int colorMode = Options.getColorMode(options);
        int mode = ZOOM_COLOR_NORMAL;
        
        if (colorMode == Options.COLOR_MODE_GREEN_ON_BLACK) {
        	mode = ZOOM_COLOR_GREEN;
        }
        else if (colorMode == Options.COLOR_MODE_RED_ON_BLACK) {
        	mode = ZOOM_COLOR_RED;
        }

        // the zoom buttons
    	if (zoomLayout != null) {
    		activityLayout.removeView(zoomLayout);
    	}
    	
        zoomLayout = new LinearLayout(this);
        zoomLayout.setOrientation(LinearLayout.HORIZONTAL);
		zoomDownButton = new ImageButton(this);
		zoomDownButton.setImageDrawable(getResources().getDrawable(zoomDownId[mode]));
		zoomDownButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomDownButton, (int)(80 * metrics.density), (int)(50 * metrics.density));	// TODO: remove hardcoded values
		zoomWidthButton = new ImageButton(this);
		zoomWidthButton.setImageDrawable(getResources().getDrawable(zoomWidthId[mode]));
		zoomWidthButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomWidthButton, (int)(58 * metrics.density), (int)(50 * metrics.density));
		zoomUpButton = new ImageButton(this);		
		zoomUpButton.setImageDrawable(getResources().getDrawable(zoomUpId[mode]));
		zoomUpButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomUpButton, (int)(80 * metrics.density), (int)(50 * metrics.density));
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        setZoomButtonHandlers();
		activityLayout.addView(zoomLayout,lp);
    }
    
    private void findText(String text) {
    	Log.d(TAG, "findText(" + text + ")");
    	this.findText = text;
    	this.find(true);
    }
    
    /**
     * Called when user presses "next" button in find panel.
     */
    private void findNext() {
    	this.find(true);
    }

    /**
     * Called when user presses "prev" button in find panel.
     */
    private void findPrev() {
    	this.find(false);
    }
    
    /**
     * Called when user presses hide button in find panel.
     */
    private void findHide() {
    	if (this.pagesView != null) this.pagesView.setFindMode(false);
    	this.currentFindResultNumber = null;
    	this.currentFindResultPage = null;
    	this.findButtonsLayout.setVisibility(View.GONE);
    }

    /**
     * Helper class that handles search progress, search cancelling etc.
     */
	static class Finder implements Runnable, DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
		private OpenFileActivity parent = null;
		private boolean forward;
		private AlertDialog dialog = null;
		private String text;
		private int startingPage;
		private int pageCount;
		private boolean cancelled = false;
		/**
		 * Constructor for finder.
		 * @param parent parent activity
		 */
		public Finder(OpenFileActivity parent, boolean forward) {
			this.parent = parent;
			this.forward = forward;
			this.text = parent.findText;
			this.pageCount = parent.pagesView.getPageCount();
			if (parent.currentFindResultPage != null) {
				if (forward) {
					this.startingPage = (parent.currentFindResultPage + 1) % pageCount;
				} else {
					this.startingPage = (parent.currentFindResultPage - 1 + pageCount) % pageCount;
				}
			} else {
				this.startingPage = parent.pagesView.getCurrentPage();
			}
		}
		public void setDialog(AlertDialog dialog) {
			this.dialog = dialog;
		}
		public void run() {
			int page = -1;
			this.createDialog();
			this.showDialog();
			for(int i = 0; i < this.pageCount; ++i) {
				if (this.cancelled) {
					this.dismissDialog();
					return;
				}
				page = (startingPage + pageCount + (this.forward ? i : -i)) % this.pageCount;
				Log.d(TAG, "searching on " + page);
				this.updateDialog(page);
				List<FindResult> findResults = this.findOnPage(page);
				if (findResults != null && !findResults.isEmpty()) {
					Log.d(TAG, "found something at page " + page + ": " + findResults.size() + " results");
					this.dismissDialog();
					this.showFindResults(findResults, page);
					return;
				}
			}
			/* TODO: show "nothing found" message */
			this.dismissDialog();
		}
		/**
		 * Called by finder thread to get find results for given page.
		 * Routed to PDF instance.
		 * If result is not empty, then finder loop breaks, current find position
		 * is saved and find results are displayed.
		 * @param page page to search on
		 * @return results 
		 */
		private List<FindResult> findOnPage(int page) {
			if (this.text == null) throw new IllegalStateException("text cannot be null");
			return this.parent.pdf.find(this.text, page);
		}
		private void createDialog() {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					String title = Finder.this.parent.getString(R.string.searching_for).replace("%1$s", Finder.this.text);
					String message = Finder.this.parent.getString(R.string.page_of).replace("%1$d", String.valueOf(Finder.this.startingPage)).replace("%2$d", String.valueOf(pageCount));
			    	AlertDialog.Builder builder = new AlertDialog.Builder(Finder.this.parent);
			    	AlertDialog dialog = builder
			    		.setTitle(title)
			    		.setMessage(message)
			    		.setCancelable(true)
			    		.setNegativeButton(R.string.cancel, Finder.this)
			    		.create();
			    	dialog.setOnCancelListener(Finder.this);
			    	Finder.this.dialog = dialog;
				}
			});
		}
		public void updateDialog(final int page) {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					String message = Finder.this.parent.getString(R.string.page_of).replace("%1$d", String.valueOf(page)).replace("%2$d", String.valueOf(pageCount));
					Finder.this.dialog.setMessage(message);
				}
			});
		}
		public void showDialog() {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					Finder.this.dialog.show();
				}
			});
		}
		public void dismissDialog() {
			final AlertDialog dialog = this.dialog;
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					dialog.dismiss();
				}
			});
		}
		public void onCancel(DialogInterface dialog) {
			Log.d(TAG, "onCancel(" + dialog + ")");
			this.cancelled = true;
		}
		public void onClick(DialogInterface dialog, int which) {
			Log.d(TAG, "onClick(" + dialog + ")");
			this.cancelled = true;
		}
		private void showFindResults(final List<FindResult> findResults, final int page) {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					int fn = Finder.this.forward ? 0 : findResults.size()-1;
					Finder.this.parent.currentFindResultPage = page;
					Finder.this.parent.currentFindResultNumber = fn;
					Finder.this.parent.pagesView.setFindResults(findResults);
					Finder.this.parent.pagesView.setFindMode(true);
					Finder.this.parent.pagesView.scrollToFindResult(fn);
					Finder.this.parent.findButtonsLayout.setVisibility(View.VISIBLE);					
					Finder.this.parent.pagesView.invalidate();
				}
			});
		}
	};
    
    /**
     * GUI for finding text.
     * Used both on initial search and for "next" and "prev" searches.
     * Displays dialog, handles cancel button, hides dialog as soon as
     * something is found.
     * @param 
     */
    private void find(boolean forward) {
    	if (this.currentFindResultPage != null) {
    		/* searching again */
    		int nextResultNum = forward ? this.currentFindResultNumber + 1 : this.currentFindResultNumber - 1;
    		if (nextResultNum >= 0 && nextResultNum < this.pagesView.getFindResults().size()) {
    			/* no need to really find - just focus on given result and exit */
    			this.currentFindResultNumber = nextResultNum;
    			this.pagesView.scrollToFindResult(nextResultNum);
    			this.pagesView.invalidate();
    			return;
    		}
    	}

    	/* finder handles next/prev and initial search by itself */
    	Finder finder = new Finder(this, forward);
    	Thread finderThread = new Thread(finder);
    	finderThread.start();
    }
}

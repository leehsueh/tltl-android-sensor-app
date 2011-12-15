package edu.stanford.tltl;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SensorDataListActivity extends ListActivity{
	private SensorDataDB mDB;
	private SimpleCursorAdapter mCursorAdapter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_saved_data_list);
        
        // Start up DB connection (closed in onDestroy).
        mDB = new SensorDataDB(this);
        mDB.open();

        // Get the "all rows" cursor. startManagingCursor() is built in for the common case,
        // takes care of closing etc. the cursor.
		Cursor cursor = mDB.queryAll(); 
		startManagingCursor(cursor);

		// Adapter: maps cursor keys, to R.id.XXX fields in the row layout.
		String[] from = new String[] { SensorDataDB.KEY_TITLE, SensorDataDB.KEY_TIMESTAMP, SensorDataDB.KEY_NOTES };
		int[] to = new int[] { R.id.dataTitle, R.id.dataTimestamp, R.id.dataNotes };
		mCursorAdapter = new SimpleCursorAdapter(this, R.layout.sensor_saved_data_list_row, cursor, from, to);
		
		// Map timestamp value to formatted date string
		mCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == SensorDataDB.INDEX_TIMESTAMP) {
					long timestamp = cursor.getLong(SensorDataDB.INDEX_TIMESTAMP);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String dateText = sdf.format(new Date(timestamp));
					TextView formattedDateView = (TextView)view;
					formattedDateView.setText(dateText);
					return true;  // i.e. we handled it
			    }
			    return false;  // i.e. the system should handle it
			}
		});
		
		setListAdapter(mCursorAdapter);
		registerForContextMenu(getListView());
    }
	
	@Override
 	protected void onDestroy() {
 		super.onDestroy();
 		mDB.close();
 	}
	
	@Override
 	protected void onListItemClick(ListView l, View v, int position, long rowId) {
 		super.onListItemClick(l, v, position, rowId);
		startDetail(rowId, false);
 	}
 	
 	public static final String EXTRA_ROWID = "rowid";
 	
 	// Starts the detail activity, either edit existing or create new.
 	public void startDetail(long rowId, boolean create) {
 		Intent intent = new Intent(this, SensorDataDetailActivity.class);
 		// Our convention: add rowId to edit existing. To create add nothing.
 		if (!create) {
 			intent.putExtra(EXTRA_ROWID, rowId);
 		}
 		startActivity(intent);
 	}
 	
 	/* Context menu stuff */
    // Create context menu for click-hold in list.
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sensor_saved_data_list_menu, menu);
	}
    
    // Context menu item-select.
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.menu_detail:
				startDetail(info.id, false);
				return true;
				
			case R.id.menu_delete:
				remove(info.id);
				return true;				
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	// Removes the given rowId from the database, updates the UI.
    public void remove(long rowId) {
		mDB.deleteRow(rowId);
		//mCursorAdapter.notifyDataSetChanged();  // confusingly, this does not work
		mCursorAdapter.getCursor().requery();  // need this
    }

}

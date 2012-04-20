package cx.pdf.android.pdfview;


import java.util.List;

import cx.pdf.android.pdfview.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * PDF Annotation: Create an item of annotation list
 * Date of last change: 20.04.2012
 */
public class AnnotationListAdapter extends BaseAdapter implements OnClickListener {
    private Context context;
    private List<AnnotationList> listAnnotation;

    // class constructor
    public AnnotationListAdapter(Context context, List<AnnotationList> listPhonebook) {
        this.context = context;
        this.listAnnotation = listPhonebook;
    }

    // get count of annotations on list
    public int getCount() {
        return listAnnotation.size();
    }

    // get selected item of annotations list
    public Object getItem(int position) {
        return listAnnotation.get(position);
    }

    // get identifier of annotation
    public long getItemId(int position) {
        return position;
    }

    /**
     * Set view to show annotation list
     */
    public View getView(int position, View convertView, ViewGroup viewGroup) {
    	AnnotationList entry = listAnnotation.get(position);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.annot_item, null);
        }

        // subject of annotation
        TextView itemSubject = (TextView) convertView.findViewById(R.id.itemSubject);
        if (entry.getSubject() != null)
        	itemSubject.setText(entry.getSubject());
        else
        	itemSubject.setVisibility(View.GONE);

        // author of annotation
        TextView itemAuthor = (TextView) convertView.findViewById(R.id.itemAuthor);
        if (entry.getAuthor() != null)
        	itemAuthor.setText(entry.getAuthor());
        else
        	itemAuthor.setVisibility(View.GONE);
        
        // contents of annotation
        TextView itemContents = (TextView) convertView.findViewById(R.id.itemContents);
        if (entry.getContents() != null)
        	itemContents.setText(entry.getContents());
        else
        	itemContents.setVisibility(View.GONE);
        
        // date of modified annotation
        TextView itemModdate = (TextView) convertView.findViewById(R.id.itemDate);
        if (entry.getModdate() != null)
        	itemModdate.setText(entry.getModdate());
        else
        	itemModdate.setVisibility(View.GONE);
        
        // page of annotation
        TextView itemPage = (TextView) convertView.findViewById(R.id.itemPage);
        if (entry.getPage() > 0)
        	itemPage.setText("page: " + Integer.toString(entry.getPage()));
        else
        	itemPage.setVisibility(View.GONE);
        return convertView;
    }


	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

}
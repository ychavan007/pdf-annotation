package cx.pdf.android.lib.pagesview;

import java.util.Map;

import android.graphics.Bitmap;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;

/**
 * Allow renderer to notify view that new bitmaps are ready.
 * Implemented by PagesView.
 */
public interface OnImageRenderedListener {
	void onImagesRendered(Map<Tile,Bitmap> renderedImages);
	void onRenderingException(RenderingException reason);
	void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo);
}

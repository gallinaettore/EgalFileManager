package it.Ettore.egalfilemanager.imageviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;


/**
 * ViewPager da utilizzare quando al suo interno si visualizzerà una PhotoView
 *
 * There are some ViewGroups (ones that utilize onInterceptTouchEvent) that throw exceptions when a PhotoView is placed within them, most notably ViewPager and DrawerLayout.
 * This is a framework issue that has not been resolved. In order to prevent this exception (which typically occurs when you zoom out), take a look at HackyDrawerLayout and
 * you can see the solution is to simply catch the exception. Any ViewGroup which uses onInterceptTouchEvent will also need to be extended and exceptions caught.
 * Use the HackyDrawerLayout as a template of how to do so.
 */
public class ViewPagerForPhotoView extends ViewPager {


    public ViewPagerForPhotoView(Context context) {
        super(context);
    }


    public ViewPagerForPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * La PhotoView può generare delle eccezione che vengono catturate e non mandano in crash l'applicazione
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            //uncomment if you really want to see these errors
            //e.printStackTrace();
            return false;
        }
    }
}

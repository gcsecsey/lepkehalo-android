package hu.csecsey.gergely.lepkehalo;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

/**
 * package ${PACKAGE}
 * Created by gcsecsey on 04/04/2018.
 */
public class DodgeFabBehavior extends FloatingActionButton.Behavior {

    public DodgeFabBehavior() {
        super();
    }

    public DodgeFabBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean getInsetDodgeRect(
            @NonNull CoordinatorLayout parent,
            @NonNull FloatingActionButton child,
            @NonNull Rect rect) {
        return false;
    }

}

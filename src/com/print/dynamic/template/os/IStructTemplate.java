/**
 * 
 */
package com.print.dynamic.template.os;

import java.util.Vector;

import com.print.dynamic.component.ViewException;
import com.print.dynamic.core.ViewPage;

import android.view.View;

/**
 * @author DongXiaoping
 *
 */
public interface IStructTemplate {
	public Vector<View> rewind(ViewPage viewPage) throws ViewException;
}

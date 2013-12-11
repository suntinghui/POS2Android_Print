package com.print.dynamic.component.os;

import android.widget.TextView;

import com.print.dynamic.component.Component;
import com.print.dynamic.component.ViewException;
import com.print.dynamic.core.ViewPage;

public class OSLabel extends StructComponent {
	
	public OSLabel(ViewPage viewPage) {
		super(viewPage);
	}

	@Override
	public TextView toOSComponent() throws ViewException {
		TextView text = new TextView(this.getContext());
		text.setTag(this.getId());
		text.setText(null == this.getValue() ? "" : this.getValue().toString().replaceAll("/n", "\n"));
//		text.setText("功能说明:\n用于查询客户当前选中账户的余额。");
		return text;
	}

	@Override
	protected Component construction(ViewPage viewPage) {
		return new OSLabel(viewPage);
	}
}

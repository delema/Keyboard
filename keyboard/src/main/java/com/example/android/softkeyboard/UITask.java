package com.example.android.softkeyboard;

import android.os.AsyncTask;

public abstract class UITask<Params, Result>
extends AsyncTask<Params, Integer, Result> {

	protected UIHandler handler = null;

	public UITask(UIHandler handler) {
		super();
		this.handler = handler;
	}
	
    protected abstract Result doInBackground(Params... params);

    protected abstract void onPostExecute(Result result);

	@Override
    protected void onPreExecute()
	{}

	@Override
    protected void onProgressUpdate(Integer... values)
	{}

	@Override
	protected void onCancelled()
	{}
}

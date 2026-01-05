package com.google.amara.chattab;

public class MyCustomObject {
    public interface MyCustomObjectListener {
        //public void onObjectReady(String title);
        public void onDataLoaded(String data);
    }
    public MyCustomObjectListener listener;

    public MyCustomObject() {
        // set null or default listener or accept as argument to constructor
        this.listener = null;
        loadDataAsync();
    }

    // Assign the listener implementing events interface that will receive the events
    public void setCustomObjectListener(MyCustomObjectListener listener) {
        this.listener = listener;
    }

    public void loadDataAsync() {
        Thread thread = new Thread(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i++;
                    if ((listener != null) & (i > 10)) {
                        listener.onDataLoaded("Hello the world");
                        break;
                    }
                }
            }
        });
        thread.start();
    }
}

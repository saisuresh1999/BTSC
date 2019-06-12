package com.example.btsc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    MediaPlayer mPlayer;

    Button listen,send,listDevices;
    ListView listView;
    TextView msg_box,status;
    EditText writeMsg;
    private ArrayAdapter aAdapter;



    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    SendRecieve sendRecieve;

    static final int STATIC_LISTENING=1;
    static final int STATIC_CONNECTING=2;
    static final int STATIC_CONNECTED=3;
    static final int STATIC_CONNECTION_FAILED=4;
    static final int STATIC_MESSAGE_RECEIVED=5;

    static final int PLAY=6;
    static final int NEXT=7;
    static final int STOP=8;

    int REQUEST_ENABLE_BLUETOOTH=1;

    private  static  final  String APP_NAME="BTSC";
    private static final UUID MY_UUID=UUID.fromString("17eb838c-bc2e-4848-9ff9-b4b38ec78ce6");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayer = MediaPlayer.create(MainActivity.this, R.raw.sample);


        listen=(Button) findViewById(R.id.listen);
        listDevices=(Button) findViewById(R.id.listDevices);
        send=(Button) findViewById(R.id.send);
        listView=(ListView) findViewById(R.id.listView);
        msg_box=(TextView) findViewById(R.id.msg);
        status=(TextView) findViewById(R.id.status);

       bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);

        }

        implementListener();



    }

    private void implementListener(){
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(bluetoothAdapter==null){
                    Toast.makeText(getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
                }
                else{
                    int index=0;
                    Set<BluetoothDevice> bt =bluetoothAdapter.getBondedDevices();
                    ArrayList list = new ArrayList();
                    btArray =new BluetoothDevice[bt.size()];
                    if(bt.size()>0){
                        for(BluetoothDevice device: bt){
                            String devicename = device.getName();

                            list.add("Name: "+devicename);
                            btArray[index]=device;
                            index++;
                        }
                        listView = (ListView) findViewById(R.id.listView);
                        aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                        listView.setAdapter(aAdapter);

                }
                }
            }
        });


listen.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        ServerClass serverClass=new ServerClass();
        serverClass.start();
    }
});
       listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               ClientClass clientClass=new ClientClass(btArray[position]);
               clientClass.start();
               status.setText("Connecting");
           }
       });


        send.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                  String string= "PLAY";
                  sendRecieve.write(string.getBytes());
        }});
    }

    Handler handler= new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case STATIC_LISTENING:
                    status.setText("Listening");
                    break;
                case STATIC_CONNECTING:
                    status.setText("Conenecting");
                    break;
                case STATIC_CONNECTED:
                    status.setText("Connnectd");
                    break;
                case STATIC_CONNECTION_FAILED:
                    status.setText("Connctin failed");
                    break;
                case STATIC_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    msg_box.setText(tempMsg);
                    break;
            }


            return true;
        }
    });



    private  class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket socket=null;
            while (socket==null){
                try {

                    Message message=Message.obtain();
                    message.what=STATIC_CONNECTING;
                    handler.sendMessage(message);


                    socket=serverSocket.accept();

                } catch (IOException e) {
                    Toast.makeText(MainActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                    Message message=Message.obtain();
                    message.what=STATIC_CONNECTION_FAILED;
                    handler.sendMessage(message);

                    e.printStackTrace();
                }
                if(socket!=null){

                    Message message=Message.obtain();
                    message.what=STATIC_CONNECTED;
                    handler.sendMessage(message);

                   //write wrote for send and recieve
                    sendRecieve=new SendRecieve(socket);
                    sendRecieve.start();

                    break;
                }


            }
        }


    }


    private class ClientClass extends Thread{
        private BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;
        public ClientClass(BluetoothDevice device1){
            bluetoothDevice=device1;

            try {
                bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                bluetoothSocket.connect();

                Message message=Message.obtain();
                message.what=STATIC_CONNECTED;
                handler.sendMessage(message);

                sendRecieve=new SendRecieve(bluetoothSocket);
                sendRecieve.start();

            } catch (IOException e) {


                e.printStackTrace();

                Message message=Message.obtain();
                message.what=STATIC_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }





        private class SendRecieve extends Thread{


        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecieve(BluetoothSocket socket){
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run(){
            byte[] buffer =new byte[1024];
            int bytes;
            while(true){
                try {
                   bytes= inputStream.read(buffer);
                   handler.obtainMessage(STATIC_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);




            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}

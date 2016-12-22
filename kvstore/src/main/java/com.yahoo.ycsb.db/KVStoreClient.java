package com.yahoo.ycsb.db;

import Server.Shared.ExchangeObjects.Request;
import Server.Shared.ExchangeObjects.RequestType;
import Server.Shared.ExchangeObjects.Response;
import Server.Shared.ExchangeObjects.ResponseType;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.Status;
import com.yahoo.ycsb.StringByteIterator;
import org.javatuples.Pair;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

/**
 * Created by Berkin GÜLER (bguler15@ku.edu.tr) on 25.08.2016.
 */
public class KVStoreClient extends DB {

  private Socket clientSocket;
  private ObjectOutput objectOutput;
  private ObjectInput objectInput;

  @Override
  public void init() {
    try {
      System.out.print("Connecting...");
      this.clientSocket = new Socket("saturn.planetlab.carleton.ca", 1881);
      System.out.println("OK");
      this.objectOutput = new ObjectOutputStream(this.clientSocket.getOutputStream());
      this.objectInput = new ObjectInputStream(this.clientSocket.getInputStream());
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
    key = createKey(table, key);
    Request request = new Request(RequestType.PULL, new Pair<>(key, null));
    Response response;
    try {
      this.objectOutput.writeObject(request);
      response = (Response) this.objectInput.readObject();
      String value = response.getResponseValue();
      result.put(key, new StringByteIterator(value));
      return Status.OK;
    } catch (IOException | ClassNotFoundException ex) {
      ex.printStackTrace();
    }
    return Status.ERROR;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status update(String table, String key, HashMap<String, ByteIterator> values) {
    return this.insert(table, key, values);
  }

  @Override
  public Status insert(String table, String key, HashMap<String, ByteIterator> values) {
    key = createKey(table, key);
    Request request = new Request(RequestType.PUSH, new Pair<>(key, createSingleValue(values)));
    Response response;
    try {
      this.objectOutput.writeObject(request);
      response = (Response) this.objectInput.readObject();
      if(response.getResponseValue().equals("OK")) {
        return Status.OK;
      }
    } catch (IOException | ClassNotFoundException ex) {
      ex.printStackTrace();
    }
    return Status.ERROR;
  }

  @Override
  public Status delete(String table, String key) {
    return Status.NOT_IMPLEMENTED;
  }

  private static String createKey(String table, String key) {
    return String.format("%s-%s", table, key);
  }

  private static String createSingleValue(HashMap<String, ByteIterator> values) {
    StringBuilder resBuilder = new StringBuilder();
    HashMap<String, String> strVals = StringByteIterator.getStringMap(values);
    for(String k : strVals.keySet()) {
      resBuilder.append(k).append("=").append(strVals.get(k)).append(",");
    }
    return resBuilder.toString();
  }
}

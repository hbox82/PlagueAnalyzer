/*
 * Creator:
 * 04.07.22 17:53 holger
 *
 * Maintainer:
 * holger
 *
 * Last Modification:
 * $Id:$
 *
 * Copyright (c) 2000 - 2022 Abacus Research AG, All Rights Reserved
 */
package com.plaguenft.stakinglegacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StakingLegacyRunner {

  private static int offset = 6000;
  private static String contract = "0xe36C3651a59e2866c0d36De6d3fb9E93dD4eF5ca";


  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }

  public static void main(String[] args) throws Exception {
    String apikey = args[0];

    String[] urls = new String[]{
        "https://api.etherscan.io/api?module=account&action=tokennfttx&contractaddress="+contract+"&page=1&offset="+offset+"&sort=asc&apikey="+apikey,
        "https://api.etherscan.io/api?module=account&action=tokennfttx&contractaddress="+contract+"&page=1&offset="+offset+"&sort=desc&apikey="+apikey
    };

    TreeMap<Integer, List<JSONObject>> map = new TreeMap<>();

    int entries = 0;
    boolean firstStep = true;
    for (String url : urls) {
      JSONObject json = readJsonFromUrl(url);
      final JSONArray result = json.getJSONArray("result");

      List<Integer> process = new ArrayList<>();

      JSONObject jsonObject;
      for (int i = 0; i < result.length(); i++) {
        jsonObject = result.getJSONObject(i);

        Integer key = Integer.parseInt((String) jsonObject.get("blockNumber"));

        boolean cont = firstStep;
        if (!firstStep) {
          if (process.contains(key)) {
            cont = true;
          } else {
            if (!map.containsKey(key)) {
              process.add(key);
              cont = true;
            }
          }
        }

        if (cont) {
          List<JSONObject> objList = map.get(key);
          if (objList == null) {
            objList = new ArrayList<>();
          }
          objList.add(jsonObject);
          entries++;
          map.put(key, objList);
        }
      }

      firstStep = false;
      Thread.sleep(1000);
    }

    System.out.println("Entries: " + entries);

    TreeMap<String, List<String>> holdersMap = new TreeMap<>();
    String address = "0x0000000000000000000000000000000000000000";

    Integer stakedFrogs = 0;
    for (Integer block : map.keySet()) {
      final List<JSONObject> blockTAs = map.get(block);
      for (JSONObject blockTA : blockTAs) {
        String from = (String) blockTA.get("from");
        String to = (String) blockTA.get("to");
        String frog = (String) blockTA.get("tokenID");


        if (from.equals(address)) {
          List<String> frogs = holdersMap.get(to);
          // staked
          if (frogs == null) {
            frogs = new ArrayList<>();
          }
          frogs.add(frog);
          stakedFrogs++;
          holdersMap.put(to, frogs);
        } else if (to.equals(address)) {
          List<String> frogs = holdersMap.get(from);
          // unstaked
          if (frogs == null || !frogs.contains(frog)) {
          } else {
            frogs.remove(frog);
            stakedFrogs--;
            if (frogs.size() == 0) {
              holdersMap.remove(from);
            } else {
              holdersMap.put(from, frogs);
            }
          }
        } else {
          System.out.println("FEHLER: " + blockTA.toString());
        }
      }
    }


    List<String> holdersStaked = getOldHolders();

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String holder : holdersMap.keySet()) {
      sb.append((first ? "" : ",") + "\"" + holder + "\"");
      first = false;
      holdersStaked.remove(holder);
    }
    System.out.println("\r\n\r\n" + sb.toString());

    System.out.println("Staked frogs: " + stakedFrogs);
    System.out.println("Unique holders: " + holdersMap.size());
    System.out.println("Holders restaked since yesterday: " + holdersStaked.size() + "\r\n");
    System.out.println(holdersStaked.toString().replace("[", "").replace("]", "").replace(", ", "\r\n"));
  }

  private static List<String> getOldHolders() {
    List<String> strings = new ArrayList<>(Arrays.asList(
            "0x01edeb06c27228e5fbdf6790d1f3f0ae88d26142", "0x028b0363ffd8c9dc545a4eb60c085177cd31436b", "0x07c20c4549db12cd27854d0cc21f1c34712d6301", "0x0a232102f42431fc4699ade6e8e717b28f0aa251", "0x0c59c59f234cbd643e5ad32b0c32cd2f71cbb7dd", "0x13dbd6eaeb20304db878e44f5e23747bc9d23ef6", "0x15e0891e44519c38d3b7ab82bb8c8e53a30bc1eb", "0x16e2f0d89f0dc2a8f3f6c823a2299877b08d565d", "0x1a1995aece8077c0209f0064a47d7e8033b2aa4f", "0x1d63d569c1e02a9ea187905f95d47b7701aae3a3", "0x1f568219f68ece8bb64c119a69217a518a13beb6", "0x1f60c10a95ac4cd9dd573efb3aeec6ac1154b353", "0x1f70aa54bdec18b0a4937fb5fb3d2738718157a4", "0x2454efc0c1e27f727d39d0b9bc0135e77de1a192", "0x253d24fc1237a4227b639dfbebb4f4290b39e08e", "0x27cb2d96add8f2e40522e9c381ce53d9d4eb7841", "0x34e7faeca9d2213dbbcac9fbe12f034705b0764b", "0x3563aad10a601d8c810bd79a8820d292f1998eed", "0x3a40dc0e94d890a9a079814af7cbcc928f046e25", "0x3d1cfca138deba20014a767ea56e0d402ad7210b", "0x3d34bd7382b63118c19394c7cae4d6e5b7e54119", "0x4d246e41d7131ff60cd533c13bd2aafce7ab1265", "0x4dace5a18a8c9d4d5f0a01269df49e3cc2b52750", "0x50fa3af4c8a7ff5c18f1524b260e7dede44c0c95", "0x523be78c0a48bbec0ad55ad94360f253026b5621", "0x55b0f37558085593088a4011750849181869a93c", "0x59292c1726ab4819f33dc2db4d1c45527364fca8", "0x5c19d0ffb70da42ea5089ebdd80e6d42f0f06f60", "0x5c518d80b4a2727db521856cc34081a5a0424643", "0x5d2ed948c0e47c53469674cd6e8d56a60698b56e", "0x5f5b47a4c5c738248001ecf14acffa58550884db", "0x6566a937e7f2177341e1830add8aa7723c4126f8", "0x68d53e63aef77b43c0a2db56edfeaadd6b26d093", "0x6a123b948adf5de41be5f3cad6d55662cc3ff568", "0x6b51c1eb9aae5fc545f1135b6ab806e3ce1c8824", "0x705204bbc56eaba1f5a2e07aa684b524cc36f37f", "0x70a5e0068a63c72cf55abd7385e61fdeddcc6067", "0x73e9cb1a6c7c4d40c22873ba71e9fbd77b6653d2", "0x74392e47033d5472bbede0c530e32779e3461fbf", "0x7789571edfb42a3edda9aa513b646ddb0b4ee4c9", "0x77ccd1ff0f054d46e67ef45c866f756db05aeb32", "0x7a94342fc568e6e057c082765b81f4e3f16640d7", "0x7d3206271fddc093ac6b62d95b0fba40ff1ae854", "0x8728c811f93eb6ac47d375e6a62df552d62ed284", "0x88c10f82cbbb803329cf16fe9e0795223e02085d", "0x8b4b6f0905718fd45862512974edb91cb28198f0", "0x8d384ca5e5e07f1afae406158cb9930a11cfbc51", "0x8d7b92145878dc8ed0c91fab70a8a8913acc0ed3", "0x914d57098c39e5d541928ec32d3102f7986c4d46", "0x9938bac0e091fa3ee4cf0e46eb7802a16b06c8c7", "0x9f94200f0cafbe72fe7dbc43c9d659182d041a8b", "0xa00f45740d2415edee0c9b22b5fa6759a3ab3f78", "0xa02bfd4376c12983a9749e609b209a87ed3e287c", "0xa12bc0e67378cf61a6a519538554d5afea0802dc", "0xa1c15a41681ea72a4ca5e0cef6c7b5f52cffb2ff", "0xaaef3c73e61b47de6f59d00a793e89d1e76c5a58", "0xad71353fe6ec7ff16c603f87b06722f15c7c2ec2", "0xae9352a27cb1dee7dd291c264da9f556550bc04d", "0xaeaf81161c6289673d95df5efbe4bb86a26d2e2c", "0xb5c2a0c46d0856c859150ef8e74c6e79ae2c6bb8", "0xb776b762bad1c2c885f0a083eb446be5e92d4722", "0xb8b1b87a1e01c5d1b03ac33dbcac459e21190584", "0xc09094131452b376b7b044f06188cc35998dd490", "0xc3efb1b3bede144f805aa3cacf5299dabc683db5", "0xc62840f0b82a6a304f21b8196322813819dc8263", "0xc79e1e75c2e5c52f694d75b27825953b7c7c73b5", "0xca808bf81ab2936bec73eb6aae5dc242e2977a8e", "0xcc43a03af87420789e043122093c600f2dafbbcb", "0xcfd767f1337ebac2cf6cd73593df2d4f9c4430ac", "0xd257606e6ea318b5c4e3b9d8c3f50a336fc12b50", "0xd379b046ca62abb38f9a544c6d6c446b515e46f5", "0xd414cc42f41e5df7879cc9acc5343a2c42b21d26", "0xd5d1b56e6b4d0694f00e7f7fc823b8d623a343b5", "0xd76a50680eb1cce6a848ec615d482147be10740d", "0xd9cd235fcf2d90e0bef1ec1e84bad1ed3f2dcbba", "0xe5d9d80aa1c4e7d8925f3163deb00180d057605d", "0xe611ded16e02dc75c139d15b4bc30dd4f29c32ea", "0xec593a7b6573127832b3b21e9c40eb4abce563e6", "0xf43b5b8145651bad10da268ef5d740e923d456ce", "0xf461921fdccc41cc32628d820b417b763e12ef69", "0xf8c4e78ac708c79b4d130720ccaaf872739f0186"
    ));
    return strings;
  }

}

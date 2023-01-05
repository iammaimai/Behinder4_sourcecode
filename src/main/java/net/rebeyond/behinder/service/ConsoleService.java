package net.rebeyond.behinder.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.rebeyond.behinder.core.Constants;
import net.rebeyond.behinder.core.IShellService;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONObject;

public class ConsoleService {
   private IShellService currentShellService;
   private JSONObject shellEntity;
   private List<Thread> workList;
   private Map<String, Method> methodCache = new HashMap();
   private int historyIndex;
   private List<String> history = new ArrayList();

   public ConsoleService(IShellService shellService, JSONObject shellEntity, List<Thread> workList) {
      this.currentShellService = shellService;
      this.shellEntity = shellEntity;
      this.workList = workList;
      this.initMethodCache();
      this.initHistory();
   }

   private void initMethodCache() {
      Method[] var1 = this.currentShellService.getClass().getDeclaredMethods();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Method shellMethod = var1[var3];
         this.methodCache.put(shellMethod.getName().toLowerCase(), shellMethod);
      }

   }

   private void initHistory() {
      this.history.add("help");
   }

   public String showHelp() {
      StringBuilder helpContent = new StringBuilder();
      helpContent.append("Commands:\n");
      Method[] var2 = this.currentShellService.getClass().getDeclaredMethods();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Method shellMethod = var2[var4];
         helpContent.append(shellMethod.getName() + "\t");
      }

      return helpContent.toString();
   }

   public String parseParams() {
      return "";
   }

   public String parseCommand(String command) throws Exception {
      Pattern pattern = Pattern.compile("[\"]([^\"]+)[\"]([\\s]+|$)|([\\S]+)");
      Matcher matcher = pattern.matcher(command);

      ArrayList paramList;
      String shellAction;
      for(paramList = new ArrayList(); matcher.find(); paramList.add(shellAction)) {
         shellAction = matcher.group(1);
         if (shellAction == null) {
            shellAction = matcher.group(0);
         }
      }

      shellAction = (String)paramList.get(0);
      List<String> subList = paramList.subList(1, paramList.size());
      String[] params = (String[])subList.toArray(new String[subList.size()]);
      switch (shellAction) {
         case "runcmd":
            if (params.length == 1) {
               params = (String[])Utils.appendArray(params, ".");
            }
         case "showfile":
         default:
            String result = this.doInvoke(shellAction, params);
            return result + "\nBShell >";
      }
   }

   private String getBasicInfo() throws Exception {
      JSONObject responseObj = this.currentShellService.getBasicInfo(Utils.getRandomString((new Random()).nextInt(20)));
      String status = responseObj.getString("status");
      String msg = responseObj.getString("msg");
      if (status.equals("success")) {
         JSONObject basicInfoObj = Utils.DecodeJsonObj(new JSONObject(responseObj.getString("msg")));
         return basicInfoObj.toString();
      } else {
         throw new Exception(msg);
      }
   }

   private String doInvoke(String shellAction, String... params) throws Exception {
      String result = "";
      Method shellMethod = (Method)this.methodCache.get(shellAction);
      if (shellMethod == null) {
         throw new Exception("invalid command");
      } else {
         JSONObject responseObj = (JSONObject)shellMethod.invoke(this.currentShellService, params);
         String status = responseObj.getString("status");
         String msg = responseObj.getString("msg");
         if (status.equals("success")) {
            result = responseObj.getString("msg");
            return result;
         } else {
            throw new Exception(msg);
         }
      }
   }

   public void addHistory(String cmd) {
      cmd = cmd.trim();
      String lastCmd = (String)this.history.get(0);
      if (!cmd.equals(lastCmd) && !cmd.equals("")) {
         this.history.add(0, cmd);
      }

      this.historyIndex = 0;
   }

   public String loadHistoryCmd(int direction) {
      String currentHistoryCmd = (String)this.history.get(this.historyIndex);
      if (direction == Constants.HISTORY_DIRECTION_UP) {
         int maxHistory = this.history.size() - 1;
         if (this.historyIndex < maxHistory) {
            ++this.historyIndex;
         }
      } else if (direction == Constants.HISTORY_DIRECTION_DOWN && this.historyIndex > 0) {
         --this.historyIndex;
      }

      return currentHistoryCmd;
   }
}

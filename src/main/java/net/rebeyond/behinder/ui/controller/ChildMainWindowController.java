package net.rebeyond.behinder.ui.controller;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.rebeyond.behinder.core.Constants;
import net.rebeyond.behinder.core.ShellService;
import net.rebeyond.behinder.dao.ShellManager;
import net.rebeyond.behinder.service.PluginService;
import net.rebeyond.behinder.service.Task;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONObject;

public class ChildMainWindowController {
   @FXML
   private GridPane bShellGuiGridPane;
   @FXML
   private TabPane mainTabPane;
   @FXML
   private WebView basicInfoView;
   @FXML
   private TextField urlText;
   @FXML
   private Label statusLabel;
   @FXML
   private Label connStatusLabel;
   @FXML
   private TextArea sourceCodeTextArea;
   @FXML
   private TextArea sourceResultArea;
   @FXML
   private Button runCodeBtn;
   @FXML
   private Tab realCmdTab;
   private JSONObject shellEntity;
   private ShellService currentShellService;
   private PluginService pluginService;
   private ShellManager shellManager;
   @FXML
   private AnchorPane pluginView;
   @FXML
   private PluginViewController pluginViewController;
   @FXML
   private FileManagerViewController fileManagerViewController;
   @FXML
   private ParallelViewController parallelViewController;
   @FXML
   private ReverseViewController reverseViewController;
   @FXML
   private DatabaseViewController databaseViewController;
   @FXML
   private CmdViewController cmdViewController;
   @FXML
   private RealCmdViewController realCmdViewController;
   @FXML
   private TunnelViewController tunnelViewController;
   @FXML
   private UserCodeViewController userCodeViewController;
   @FXML
   private MemoViewController memoViewController;
   private Map<String, String> basicInfoMap = new HashMap();
   private List<Thread> workList = new ArrayList();
   private List<Task> taskList = new ArrayList();

   public void initialize() {
      this.initControls();
   }

   public List<Thread> getWorkList() {
      return this.workList;
   }

   private void initControls() {
      this.urlText.textProperty().addListener((observable, oldValue, newValue) -> {
         try {
            this.statusLabel.setText("正在获取基本信息，请稍后……");
            this.connStatusLabel.setText("正在连接");
            WebEngine webengine = this.basicInfoView.getEngine();
            Runnable runner = () -> {
               try {
                  this.doConnect();
                  int randStringLength = (new SecureRandom()).nextInt(3000);
                  String randString = Utils.getRandomString(randStringLength);
                  JSONObject basicInfoObj = new JSONObject(this.currentShellService.getBasicInfo(randString));
                  if (basicInfoObj.has("msg")) {
                     basicInfoObj = Utils.DecodeAndJson(basicInfoObj.getString("msg"));
                  }

                  final String basicInfoStr = new String(Base64.getDecoder().decode(basicInfoObj.getString("basicInfo")), "UTF-8");
                  String driveList = (new String(Base64.getDecoder().decode(basicInfoObj.getString("driveList")), "UTF-8")).replace(":\\", ":/");
                  String currentPath = new String(Base64.getDecoder().decode(basicInfoObj.getString("currentPath")), "UTF-8");
                  String osInfo = (new String(Base64.getDecoder().decode(basicInfoObj.getString("osInfo")), "UTF-8")).toLowerCase();
                  String arch = (new String(Base64.getDecoder().decode(basicInfoObj.getString("arch")), "UTF-8")).toLowerCase();
                  String localIp = (new String(Base64.getDecoder().decode(basicInfoObj.optString("localIp", "")), "UTF-8")).toLowerCase();
                  this.basicInfoMap.put("basicInfo", basicInfoStr);
                  this.basicInfoMap.put("driveList", driveList);
                  this.basicInfoMap.put("currentPath", Utils.formatPath(currentPath));
                  this.basicInfoMap.put("workPath", Utils.formatPath(currentPath));
                  this.basicInfoMap.put("osInfo", osInfo.replace("winnt", "windows"));
                  this.basicInfoMap.put("arch", arch);
                  this.basicInfoMap.put("localIp", localIp);
                  this.shellManager.updateOsInfo(this.shellEntity.getInt("id"), osInfo);
                  Platform.runLater(new Runnable() {
                     public void run() {
                        webengine.loadContent(basicInfoStr);

                        try {
                           ChildMainWindowController.this.cmdViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.realCmdViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.pluginViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.pluginService, ChildMainWindowController.this.workList, ChildMainWindowController.this.taskList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.shellManager, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.fileManagerViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.parallelViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.pluginViewController, ChildMainWindowController.this.pluginService, ChildMainWindowController.this.workList, ChildMainWindowController.this.taskList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.shellManager, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.reverseViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.databaseViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.shellManager);
                           ChildMainWindowController.this.tunnelViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.basicInfoMap);
                           ChildMainWindowController.this.userCodeViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel);
                           ChildMainWindowController.this.memoViewController.init(ChildMainWindowController.this.currentShellService, ChildMainWindowController.this.workList, ChildMainWindowController.this.statusLabel, ChildMainWindowController.this.shellManager);
                        } catch (Exception var2) {
                        }

                        ChildMainWindowController.this.connStatusLabel.setText("已连接");
                        ChildMainWindowController.this.connStatusLabel.setTextFill(Color.BLUE);
                        ChildMainWindowController.this.statusLabel.setText("[OK]连接成功，基本信息获取完成。");
                     }
                  });
                  this.shellManager.setShellStatus(this.shellEntity.getInt("id"), Constants.SHELL_STATUS_ALIVE);
                  Runnable worker = new Runnable() {
                     public void run() {
                        while(true) {
                           try {
                              Thread.sleep((long)(((new Random()).nextInt(5) + 5) * 60 * 1000));
                              int randomStringLength = (new SecureRandom()).nextInt(3000);
                              ChildMainWindowController.this.currentShellService.echo(Utils.getRandomString(randomStringLength));
                           } catch (Exception var2) {
                              if (var2 instanceof InterruptedException) {
                                 return;
                              }

                              Platform.runLater(() -> {
                                 Utils.showErrorMessage("提示", "由于您长时间未操作，当前连接会话已超时，请重新打开该网站。");
                              });
                              return;
                           }
                        }
                     }
                  };
                  Thread keepAliveWorker = new Thread(worker);
                  keepAliveWorker.start();
                  this.workList.add(keepAliveWorker);
               } catch (final Exception var13) {
                  var13.printStackTrace();
                  Platform.runLater(new Runnable() {
                     public void run() {
                        ChildMainWindowController.this.connStatusLabel.setText("连接失败");
                        ChildMainWindowController.this.connStatusLabel.setTextFill(Color.RED);
                        ChildMainWindowController.this.statusLabel.setText("[ERROR]连接失败：" + var13.getClass().getName() + ":" + var13.getMessage());

                        try {
                           ChildMainWindowController.this.shellManager.setShellStatus(ChildMainWindowController.this.shellEntity.getInt("id"), Constants.SHELL_STATUS_DEAD);
                        } catch (Exception var2) {
                        }

                     }
                  });
               }

            };
            Thread workThrad = new Thread(runner);
            this.workList.add(workThrad);
            workThrad.start();
         } catch (Exception var7) {
         }

      });
      this.mainTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
         public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
            switch (newTab.getId()) {
               case "cmdTab":
               case "":
               default:
            }
         }
      });
   }

   private void doConnect() throws Exception {
      boolean connectResult = this.currentShellService.doConnect();
   }

   private void initTabs() {
      if (this.shellEntity.getString("type").equals("asp")) {
         Iterator var1 = this.mainTabPane.getTabs().iterator();

         while(true) {
            Tab tab;
            do {
               if (!var1.hasNext()) {
                  return;
               }

               tab = (Tab)var1.next();
            } while(!tab.getId().equals("realCmdTab") && !tab.getId().equals("tunnelTab") && !tab.getId().equals("reverseTab"));

            tab.setDisable(true);
         }
      }
   }
}

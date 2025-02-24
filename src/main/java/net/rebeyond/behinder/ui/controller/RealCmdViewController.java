package net.rebeyond.behinder.ui.controller;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.rebeyond.behinder.core.Constants;
import net.rebeyond.behinder.core.IShellService;
import net.rebeyond.behinder.dao.ShellManager;
import net.rebeyond.behinder.utils.Utils;
import netscape.javascript.JSObject;
import org.json.JSONObject;

public class RealCmdViewController {
   private ShellManager shellManager;
   @FXML
   private TextField shellPathText;
   @FXML
   private Button realCmdBtn;
   private IShellService currentShellService;
   private JSONObject shellEntity;
   private List<Thread> workList;
   private List<Thread> cmdWorkList = new ArrayList();
   Map<String, String> basicInfoMap;
   private Label statusLabel;
   private RealCmdViewController me = this;
   @FXML
   private WebView mywebview;
   private LinkedBlockingQueue<String> commandQueue;
   private boolean immediatelyRead = false;
   private int running;

   public RealCmdViewController() {
      this.running = Constants.REALCMD_STOPPED;
   }

   public void init(IShellService shellService, List<Thread> workList, Label statusLabel, Map<String, String> basicInfoMap) {
      this.currentShellService = shellService;
      this.shellEntity = shellService.getShellEntity();
      this.basicInfoMap = basicInfoMap;
      this.workList = workList;
      this.statusLabel = statusLabel;
      this.initRealCmdView();
   }

   public void receive(String input) {
      if (this.running != Constants.REALCMD_RUNNING) {
         this.statusLabel.setText("虚拟终端已停止，请先启动虚拟终端.");
      } else {
         String osInfo = (String)this.basicInfoMap.get("osInfo");
         if (Utils.getOSType(osInfo) == Constants.OS_TYPE_WINDOWS && input.getBytes()[0] == 127) {
            input = "\b \b";
         }

         this.commandQueue.offer(input);
      }
   }

   private void stopWorkers() {
      Iterator var1 = this.cmdWorkList.iterator();

      while(var1.hasNext()) {
         Thread worker = (Thread)var1.next();

         while(worker.isAlive()) {
            worker.stop();
         }
      }

      this.cmdWorkList.clear();
   }

   private void initWorkers() {
      Runnable cmdWriter = () -> {
         StringBuilder windowsCommondBuf = new StringBuilder();

         while(true) {
            while(true) {
               while(true) {
                  try {
                     String commandToExecute = (String)this.commandQueue.poll(10000L, TimeUnit.MILLISECONDS);
                     if (commandToExecute != null) {
                        String osInfo = (String)this.basicInfoMap.get("osInfo");
                        if (Utils.getOSType(osInfo) == Constants.OS_TYPE_WINDOWS) {
                           if (commandToExecute.charAt(commandToExecute.length() - 1) != '\r' && commandToExecute.charAt(commandToExecute.length() - 1) != '\n') {
                              int delIndex = commandToExecute.indexOf("\b \b");
                              if (delIndex == 0) {
                                 windowsCommondBuf.setLength(windowsCommondBuf.length() - 1);
                                 if (commandToExecute.length() > 2) {
                                    windowsCommondBuf.append(commandToExecute.substring(3));
                                 }
                              } else if (delIndex > 0) {
                                 windowsCommondBuf.append(commandToExecute.substring(0, delIndex - 1) + commandToExecute.substring(delIndex + 3));
                              } else {
                                 windowsCommondBuf.append(commandToExecute);
                              }

                              String finalCommandToExecute = commandToExecute;
                              Platform.runLater(() -> {
                                 this.write(finalCommandToExecute);
                              });
                              continue;
                           }

                           commandToExecute = commandToExecute.replace((new StringBuilder()).append('\r'), "" + '\r' + '\n');
                           windowsCommondBuf.append(commandToExecute);
                           String finalCommandToExecute1 = commandToExecute;
                           Platform.runLater(() -> {
                              this.write(finalCommandToExecute1);
                           });
                           commandToExecute = windowsCommondBuf.toString();
                           windowsCommondBuf.setLength(0);
                        } else if (commandToExecute.charAt(commandToExecute.length() - 1) == '\r' || commandToExecute.charAt(commandToExecute.length() - 1) == '\n') {
                           commandToExecute = commandToExecute.replace('\n', '\r');
                        }

                        this.currentShellService.writeRealCMD(commandToExecute);
                        this.immediatelyRead = true;
                     }
                  } catch (Exception var6) {
                  }
               }
            }
         }
      };
      Thread cmdWriterWorker = new Thread(cmdWriter);
      cmdWriterWorker.setName("cmdWriterWorker");
      this.cmdWorkList.add(cmdWriterWorker);
      this.workList.add(cmdWriterWorker);
      cmdWriterWorker.start();
      Runnable cmdReader = () -> {
         int blankCount = 0;
         int sleepCount = 0;

         while(true) {
            while(true) {
               while(true) {
                  try {
                     JSONObject resultObj = this.currentShellService.readRealCMD();
                     String status = resultObj.getString("status");
                     String msg = resultObj.getString("msg");
                     if (status.equals("fail")) {
                        Platform.runLater(() -> {
                           this.statusLabel.setText(msg);
                        });
                        Thread.sleep(200L);
                     } else if (msg.length() >= 1) {
                        blankCount = 0;
                        Platform.runLater(() -> {
                           this.write(msg);
                        });
                     } else {
                        Thread.sleep(20L);
                        ++blankCount;

                        while(blankCount > 10 && sleepCount < 20 && !this.immediatelyRead) {
                           Thread.sleep((long)(10 * (new Random()).nextInt(5)));
                           ++sleepCount;
                        }

                        sleepCount = 0;
                        if (this.immediatelyRead) {
                           blankCount = 0;
                           this.immediatelyRead = false;
                        }

                        if (blankCount > 15) {
                           while(sleepCount < 1000 && !this.immediatelyRead) {
                              Thread.sleep((long)(10 * (new Random()).nextInt(5)));
                              ++sleepCount;
                           }

                           sleepCount = 0;
                           this.immediatelyRead = false;
                        }
                     }
                  } catch (InterruptedException var6) {
                  } catch (Exception var7) {
                  }
               }
            }
         }
      };
      Thread cmdReaderWorker = new Thread(cmdReader);
      cmdReaderWorker.setName("cmdReaderWorker");
      this.cmdWorkList.add(cmdReaderWorker);
      this.workList.add(cmdReaderWorker);
      cmdReaderWorker.start();
   }

   private void setBtnIcon(String type) {
      try {
         ImageView icon = new ImageView();
         if (type.equals("start")) {
            icon.setImage(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/start.png"))));
         } else if (type.equals("stop")) {
            icon.setImage(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/stop.png"))));
         }

         icon.setFitHeight(14.0);
         icon.setPreserveRatio(true);
         this.realCmdBtn.setGraphic(icon);
      } catch (Exception var3) {
      }

   }

   private void initRealCmdView() {
      String osInfo = (String)this.basicInfoMap.get("osInfo");
      if (osInfo.indexOf("windows") < 0 && osInfo.indexOf("winnt") < 0) {
         this.shellPathText.setText("/bin/bash");
      } else {
         this.shellPathText.setText("cmd.exe");
      }

      this.setBtnIcon("start");
      this.realCmdBtn.setOnAction((event) -> {
         if (this.realCmdBtn.getText().equals("启动")) {
            this.statusLabel.setText("正在启动虚拟终端……");
            this.initCmdQueue();
            this.createRealCmd();
         } else {
            this.stopRealCmd();
            this.stopWorkers();
            this.destroyCmdQueue();
         }

      });
      WebEngine webEngine = this.mywebview.getEngine();
      this.mywebview.setContextMenuEnabled(false);
      this.createContextMenu(this.mywebview);
      webEngine.documentProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue != null) {
            JSObject window = (JSObject)webEngine.executeScript("window");
            window.setMember("app", this.me);
         }
      });
      this.mywebview.getEngine().load(this.getClass().getResource("/net/rebeyond/behinder/resource/x.htm").toExternalForm());
   }

   private void initCmdQueue() {
      this.commandQueue = new LinkedBlockingQueue();
   }

   private void destroyCmdQueue() {
      if (this.commandQueue != null) {
         this.commandQueue = null;
      }

   }

   public void copyText(String text) {
      Utils.setClipboardString(text);
   }

   private void pasteText() {
      JSObject window = (JSObject)this.mywebview.getEngine().executeScript("window");
      JSObject terminal = (JSObject)window.getMember("term");
      terminal.call("paste", new Object[]{Utils.getClipboardString()});
   }

   private void write(String text) {
      try {
         JSObject window = (JSObject)this.mywebview.getEngine().executeScript("window");
         JSObject terminal = (JSObject)window.getMember("term");
         String osInfo = (String)this.basicInfoMap.get("osInfo");
         if (Utils.getOSType(osInfo) != Constants.OS_TYPE_WINDOWS) {
            text = text.replaceFirst("\r[^\n]", "\r\n");
         }

         terminal.call("write", new Object[]{text});
      } catch (Exception var5) {
      }

   }

   private void delete(String text) {
      try {
         JSObject window = (JSObject)this.mywebview.getEngine().executeScript("window");
         JSObject terminal = (JSObject)window.getMember("t");
         JSObject htermIO = (JSObject)terminal.getMember("io");
         htermIO.call("print", new Object[]{text});
      } catch (Exception var5) {
      }

   }

   @FXML
   private void createRealCmd() {
      Runnable runner = () -> {
         try {
            final String bashPath = this.shellPathText.getText();
            (new Thread() {
               public void run() {
                  try {
                     JSONObject var1 = RealCmdViewController.this.currentShellService.createRealCMD(bashPath);
                  } catch (Exception var2) {
                     var2.printStackTrace();
                  }

               }
            }).start();
            Thread.sleep(2000L);
            this.running = Constants.REALCMD_RUNNING;
            JSONObject resultObj = this.currentShellService.readRealCMD();
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");
            Platform.runLater(() -> {
               if (status.equals("success")) {
                  this.statusLabel.setText("虚拟终端启动完成。");
                  this.initWorkers();
                  this.mywebview.requestFocus();
                  this.realCmdBtn.setText("停止");
                  this.setBtnIcon("stop");
                  this.running = Constants.REALCMD_RUNNING;
                  this.write(msg);
               } else {
                  this.statusLabel.setText("虚拟终端启动失败:" + msg);
               }

            });
         } catch (Exception var5) {
            var5.printStackTrace();
            Platform.runLater(() -> {
               this.statusLabel.setText("虚拟终端启动失败:" + var5.getMessage());
            });
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }

   private void stopRealCmd() {
      this.statusLabel.setText("正在停止虚拟终端……");
      Runnable runner = () -> {
         try {
            JSONObject resultObj = this.currentShellService.stopRealCMD();
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");
            Platform.runLater(() -> {
               if (status.equals("success")) {
                  this.statusLabel.setText("虚拟终端已停止。");
                  this.realCmdBtn.setText("启动");
                  this.running = Constants.REALCMD_STOPPED;
                  this.setBtnIcon("start");
               } else {
                  this.statusLabel.setText("虚拟终端启动失败:" + msg);
               }

            });
         } catch (Exception var4) {
            Platform.runLater(() -> {
               this.statusLabel.setText("操作失败:" + var4.getMessage());
            });
         }

      };
      Thread workThrad = new Thread(runner);
      this.workList.add(workThrad);
      workThrad.start();
   }

   private void createContextMenu(WebView webView) {
      ContextMenu contextMenu = new ContextMenu();
      MenuItem copyItem = new MenuItem("复制");
      copyItem.setOnAction((e) -> {
         this.copyText(webView.getEngine().executeScript("window.term.getSelection()").toString());
      });
      MenuItem pastItem = new MenuItem("粘贴");
      pastItem.setOnAction((e) -> {
         this.pasteText();
      });
      SeparatorMenuItem sep = new SeparatorMenuItem();
      MenuItem reload = new MenuItem("清屏");
      reload.setOnAction((e) -> {
         webView.getEngine().reload();
      });
      contextMenu.getItems().addAll(copyItem, pastItem, sep, reload);
      webView.setOnMousePressed((e) -> {
         if (e.getButton() == MouseButton.SECONDARY) {
            contextMenu.show(webView, e.getScreenX(), e.getScreenY());
         } else {
            contextMenu.hide();
         }

      });
   }
}

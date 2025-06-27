/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import type {PropsWithChildren} from 'react';
import {
  Button,
  Platform,
  ScrollView,
  StatusBar,
  StyleSheet,
  useColorScheme,
  View,
  Linking, PermissionsAndroid, Alert,
} from 'react-native';

import {
  Colors,
  Header,
} from 'react-native/Libraries/NewAppScreen';
import { NativeModules } from 'react-native';
import { pick, types } from '@react-native-documents/picker';
import RNFS from 'react-native-fs';
import { Buffer } from 'buffer';



function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const { SecureFileStorage, SecureCertificateBridge, WifiConnector, SecureDownloader, SecureHttps} = NativeModules;

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  /*
   * To keep the template simple and small we're adding padding to prevent view
   * from rendering under the System UI.
   * For bigger apps the recommendation is to use `react-native-safe-area-context`:
   * https://github.com/AppAndFlow/react-native-safe-area-context
   *
   * You can read more about it here:
   * https://github.com/react-native-community/discussions-and-proposals/discussions/827
   */
  const safePadding = '5%';
  //IoTLab@safran#1234
  //S3S1BLR5600102
  const ssid = 'SESI_LAB', password = 'S3S1BLR5600102', isWEP = false;


//  async function requestWifiPermissions() {
//   if (Platform.OS === 'android') {
//     await request(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION);
//     await request(PERMISSIONS.ANDROID.CHANGE_NETWORK_STATE);

//     const writeSettings = await check(PERMISSIONS.ANDROID.WRITE_SETTINGS);
//     if (writeSettings !== RESULTS.GRANTED) {
//       Linking.openSettings(); // or Intent to WRITE_SETTINGS
//     }
//   }
// }

 async function connectToWifi(ssid, password, isWEP = false) {
  try {
    // await requestWifiPermissions();
    const result = await WifiConnector.connectToWifi(ssid, password, isWEP);
    console.log('Wi-Fi connection result:', result);
  } catch (e) {
    console.warn('Wi-Fi error:', e);
  }
}

const connectToWifiIOS = async (ssid: string, password: string): Promise<string> => {
  return WifiConnector.connectToWifi(ssid, password);
};

async function requestStoragePermission() {
  if (Platform.OS === 'android') {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
      {
        title: 'Storage Permission',
        message: 'We need access to your storage to pick files.',
        buttonNeutral: 'Ask Me Later',
        buttonNegative: 'Cancel',
        buttonPositive: 'OK',
      }
    );

    console.log("requestStoragePermission()", granted);

    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      return true;
    } else if (granted === PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) {
      Alert.alert(
        'Permission required',
        'Storage permission was permanently denied. Please enable it in Settings.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Open Settings', onPress: () => Linking.openSettings() },
        ]
      );
    } else {
      Alert.alert('Permission Denied', 'Cannot access storage.');
    }
    return false;
  }
  return true;
}

  const pickAndStoreCertificate = async () => {
    // console.log("requestStoragePermission()", await requestStoragePermission());
    // const hasPermission = await PermissionsAndroid.check(
    //   PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE
    // );
    // if (!hasPermission) {
    //   await requestStoragePermission();
    // }
    // if(await requestStoragePermission()){
    console.log("before click");
    const [res] = await pick({
      type: types.allFiles,
    });
    const filePath = res.uri.replace('file://', '');
    console.log("file path: ", filePath);
    const certName = 'user-cert';

    if (Platform.OS === 'android') {
      console.log("inside");
      SecureCertificateBridge.storeCertificate('/storage/emulated/0/Download/key.pem', certName).then((d) => {
        console.log("called data: ", d);
      }).catch((e)=>{
        console.log("called error: ",e);
      });
    } else {
      console.log("inside ios");

      SecureCertificateBridge.storeCertificate(filePath, certName).then((d) => {
        console.log("called data: ",d);
      }).catch((e)=>{
        console.log("called error: ",e);
      });
    }
    console.log('Certificate stored securely');
  // }
  };

  async function downloadFile(filename) {
  const url = `http://192.168.0.148:8000/api/data?file=${encodeURIComponent(filename)}`;

  try {
    const res = await fetch(url);
    console.log("res", res);
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const arrayBuffer = await res.arrayBuffer();
    const base64Data = Buffer.from(arrayBuffer).toString('base64');

    // Choose destination path
    const destPath =
      Platform.OS === 'android'
        ? `${RNFS.DownloadDirectoryPath}/${filename}`
        : `${RNFS.DocumentDirectoryPath}/${filename}`; // iOS safe directory

    // Save file
    await RNFS.writeFile(destPath, base64Data, 'base64');

    console.log(`✅ File downloaded and saved to: ${destPath}`);
    return destPath;
  } catch (err) {
    console.error('❌ Download failed:', err.message);
  }
}

 async function downloadFileHttps(filename, destFileName) {
  const url = `https://192.168.0.148:8443/api/data?file=${encodeURIComponent(filename)}`;

  try {
    SecureDownloader.downloadSecureFile(url, destFileName)
      .then(path => console.log('✅ File saved at:', path))
      .catch(err => console.error('❌ Download error:', err));
  } catch (err) {
    console.error('❌ Download failed:', err);
  }
}



  return (
    <View style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        style={backgroundStyle}>
        <View style={{paddingRight: safePadding}}>
          <Header/>
        </View>

        <Button title="Save File"
        onPress={()=>{console.log('clicked save');
        SecureFileStorage.saveFile("mySecret.txt", "This is top secret for app ios")
          .then(() => console.log("Saved!"))
          .catch(err => console.error(err));
        }}
        />

        <Button title="Read File"
        onPress={()=>{console.log('clicked save');
          SecureFileStorage.readFile("mySecret.txt")
          .then(content => console.log("Decrypted content:", content))
          .catch(err => console.error(err));
        }}
        />

        <Button title="Save File from downloads"
        onPress={async ()=>{console.log('clicked save to downloads');
          const [res] = await pick();
          const sourcePath = res.uri || res.uri;
          const destFileName = 'my_encrypted_file.dat';
          console.log("destFileName", destFileName);
          console.log("sourcePath", sourcePath);
          console.log("res", res);
    const filePath = Platform.OS === 'ios' ?sourcePath.replace( 'file://', '' ): sourcePath;

        SecureFileStorage.storeFileFromPath(
          filePath,
          destFileName
        ).then((d)=>console.log(d)).catch((e)=>console.error(e));
        }}
        />

        <Button title="Read File to downloads"
        onPress={()=>{console.log('clicked read to downloads');
          const destFileName = 'my_encrypted_file.dat';
          SecureFileStorage.getDecryptedFileToDownloads(
            destFileName,
            'output.pem'
          ).then(console.log).catch(console.error);
        }}
        />

        <Button title="Store cert"
        onPress={async ()=>{console.log('clicked read to downloads');
          console.log('clicked read to downloads 2');
         await pickAndStoreCertificate();
        }}
        />

        <Button title="Read cert"
        onPress={()=>{console.log('clicked read to downloads');
          SecureCertificateBridge.readCertificate(
            'user-cert'
          ).then(async (base64Data)=>{
            // const decoded = Buffer.from(base64Data, 'base64').toString('utf-8');
            console.log('Retrieved certificate:', base64Data);
            const destPath = `${RNFS.DocumentDirectoryPath}/retrieved-key.pem`;
            console.log("dest path: ",destPath);

            await RNFS.writeFile(destPath, base64Data, 'base64');
            console.log('Decrypted cert written to Downloads.');
          }).catch(console.error);
        }}
        />
        <Button title="WIFI Connection"
        onPress={()=>{
          if (Platform.OS === 'android') {
            console.log('clicked wifi connect android');
            connectToWifi(
            ssid, password, isWEP
          ).then((d)=>{
            console.log('Wifi connected', d);
          }).catch((e)=>console.error(e));
          }else{
            console.log('clicked wifi connect IOS');
            connectToWifiIOS(
            ssid, password
          ).then((d)=>{
            console.log('Wifi connected', d);
          }).catch((e)=>console.error(e));
          }
          
        }}
        />

        <Button title="Check WIFI Connection and internet"
        onPress={()=>{console.log('clicked Check WIFI Connection and internet');
          // downloadFile('test.txt'); //'gain-1.csv.enc' //HTTP api cal
          downloadFileHttps('test.txt','testHttps.txt'); //HTTP(s) api call
        //   fetch('http://192.168.0.148:8000/api/data', {
        //     method: 'GET',
        //     headers: {
        //       'Accept': 'application/json',
        //       'Content-Type': 'application/json',
        //     },
        //   })
        //     .then(res => {
        //       console.log("Internet accessible", res )
        //       if (res.status === 204) console.log("Internet accessible", res );
        //       else console.warn("Connected but no internet");
        //     })
        //     .catch((e)=>{console.error("http error: ",e)});
        }}
        />
        <Button title="Save to downloads in android"
        onPress={()=>{console.log('clicked Save to downloads in android');
          if (Platform.OS === 'android') {
            SecureFileStorage.exportFileToDownloads(
              'testHttps.txt'
            ).then((d)=>{
              console.log('file copied...');
            }).catch(console.error);
          }
        }}
        />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;

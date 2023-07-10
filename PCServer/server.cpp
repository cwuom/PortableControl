#include <iostream>
#include <thread>
#include <winsock.h>
#pragma comment (lib,"ws2_32.lib")
#include <mmdeviceapi.h>
#include <endpointvolume.h>
#include <audioclient.h>
#include <string>

#include "rapidjson/document.h"


using namespace rapidjson;
using namespace std;

void Send(SOCKET client, char send_buf[]);
void Recv(SOCKET client, char recv_buf[]);

/**
 * @brief setVolum
 * 设置系统音量
 * @param volume
 * -2 恢复静音
 * -1 静音
 * 0~100:音量比例
 * @return
 */
bool setVolum(int level)
{
    HRESULT hr;
    IMMDeviceEnumerator* pDeviceEnumerator = 0;
    IMMDevice* pDevice = 0;
    IAudioEndpointVolume* pAudioEndpointVolume = 0;
    IAudioClient* pAudioClient = 0;

    try {
        hr = CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL, CLSCTX_ALL, __uuidof(IMMDeviceEnumerator), (void**)&pDeviceEnumerator);
        if (FAILED(hr)) throw "CoCreateInstance";
        hr = pDeviceEnumerator->GetDefaultAudioEndpoint(eRender, eMultimedia, &pDevice);
        if (FAILED(hr)) throw "GetDefaultAudioEndpoint";
        hr = pDevice->Activate(__uuidof(IAudioEndpointVolume), CLSCTX_ALL, NULL, (void**)&pAudioEndpointVolume);
        if (FAILED(hr)) throw "pDevice->Active";
        hr = pDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, NULL, (void**)&pAudioClient);
        if (FAILED(hr)) throw "pDevice->Active";

        if (level == -2) {
            hr = pAudioEndpointVolume->SetMute(FALSE, NULL);
            if (FAILED(hr)) throw "SetMute";
        }
        else if (level == -1) {
            hr = pAudioEndpointVolume->SetMute(TRUE, NULL);
            if (FAILED(hr)) throw "SetMute";
        }
        else {
            if (level < 0 || level>100) {
                hr = E_INVALIDARG;
                throw "Invalid Arg";
            }

            float fVolume;
            fVolume = level / 100.0f;
            hr = pAudioEndpointVolume->SetMasterVolumeLevelScalar(fVolume, &GUID_NULL);
            if (FAILED(hr)) throw "SetMasterVolumeLevelScalar";

            pAudioClient->Release();
            pAudioEndpointVolume->Release();
            pDevice->Release();
            pDeviceEnumerator->Release();
            return true;
        }
    }
    catch (...) {
        if (pAudioClient) pAudioClient->Release();
        if (pAudioEndpointVolume) pAudioEndpointVolume->Release();
        if (pDevice) pDevice->Release();
        if (pDeviceEnumerator) pDeviceEnumerator->Release();
        throw;
    }
    return false;
}

/**
 * @brief volume
 * 获取系统音量
 * @return
 */
int volume()
{
    HRESULT hr;
    IMMDeviceEnumerator* pDeviceEnumerator = 0;
    IMMDevice* pDevice = 0;
    IAudioEndpointVolume* pAudioEndpointVolume = 0;
    IAudioClient* pAudioClient = 0;


    try {
        CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
        hr = CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL, CLSCTX_ALL, __uuidof(IMMDeviceEnumerator), (void**)&pDeviceEnumerator);
        if (FAILED(hr)) throw "CoCreateInstance";
        hr = pDeviceEnumerator->GetDefaultAudioEndpoint(eRender, eMultimedia, &pDevice);
        if (FAILED(hr)) throw "GetDefaultAudioEndpoint";
        hr = pDevice->Activate(__uuidof(IAudioEndpointVolume), CLSCTX_ALL, NULL, (void**)&pAudioEndpointVolume);
        if (FAILED(hr)) throw "pDevice->Active";
        hr = pDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, NULL, (void**)&pAudioClient);
        if (FAILED(hr)) throw "pDevice->Active";


        float fVolume;


        hr = pAudioEndpointVolume->GetMasterVolumeLevelScalar(&fVolume);

        if (FAILED(hr)) throw "SetMasterVolumeLevelScalar";


        pAudioClient->Release();
        pAudioEndpointVolume->Release();
        pDevice->Release();
        pDeviceEnumerator->Release();


        int  intVolume = fVolume * 100;
        if (fVolume > 100)
        {
            fVolume = 100;
        }
        return intVolume;

    }


    catch (...) {
        if (pAudioClient) pAudioClient->Release();
        if (pAudioEndpointVolume) pAudioEndpointVolume->Release();
        if (pDevice) pDevice->Release();
        if (pDeviceEnumerator) pDeviceEnumerator->Release();
        throw;
    }
}

int send_volume(string addr) {
    sockaddr_in socketAddr;

    SOCKET clientSocket = {};
    clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    socketAddr.sin_family = AF_INET;
    socketAddr.sin_addr.s_addr = inet_addr(addr.c_str());
    socketAddr.sin_port = htons(4333);
    int cRes = connect(clientSocket, (SOCKADDR*)&socketAddr, sizeof(SOCKADDR));

    char sendBuf[10240] = { "\0" };
    string s = to_string(volume());
    for (int i = 0; i < s.length(); i++) {
        sendBuf[i] = s[i];
    }

    send(clientSocket, sendBuf, strlen(sendBuf), 0);
    return 0;
}


int Send(string addr, string content) {
    sockaddr_in socketAddr;

    SOCKET clientSocket = {};
    clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    socketAddr.sin_family = AF_INET;\
    socketAddr.sin_addr.s_addr = inet_addr(addr.c_str());
    socketAddr.sin_port = htons(4333);
    int cRes = connect(clientSocket, (SOCKADDR*)&socketAddr, sizeof(SOCKADDR));

    char sendBuf[10240] = { "\0" };
    string s = content;
    for (int i = 0; i < s.length(); i++) {
        sendBuf[i] = s[i];
    }

    send(clientSocket, sendBuf, strlen(sendBuf), 0);
    return 0;
}

int send_state(string addr, bool m) {
    sockaddr_in socketAddr;

    SOCKET clientSocket = {};
    clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    socketAddr.sin_family = AF_INET;
    socketAddr.sin_addr.s_addr = inet_addr(addr.c_str());
    socketAddr.sin_port = htons(4333);
    int cRes = connect(clientSocket, (SOCKADDR*)&socketAddr, sizeof(SOCKADDR));

    char sendBuf[10240] = { "\0" };
    string s = "!m = False";
    if (m) {
        s = "!m = True";
    }

    for (int i = 0; i < s.length(); i++) {
        sendBuf[i] = s[i];
    }

    send(clientSocket, sendBuf, strlen(sendBuf), 0);
    return 0;
}

void move_to(string r) {
    char c = '\0';

    r.erase(remove(r.begin(), r.end(), '\n'), r.end());

    Document document;
    document.Parse(r.c_str());
    assert(document.IsObject());


    string x = document["x"].GetString();
    string y = document["y"].GetString();
    cout << "x:" << x << " -- " << "y:" << y << endl;



    float fx = atof(x.c_str())*0.5;
    float fy = atof(y.c_str())*0.5;
    POINT p;
    GetCursorPos(&p);
    SetCursorPos(p.x + fx, p.y + fy);
}


void scroll_to(string r) {
    char c = '\0';

    r.erase(remove(r.begin(), r.end(), '\n'), r.end());
    r = r.replace(0, 3, "S::", c);
    cout << r << endl;
    
    float fy = atof(r.c_str())*1.3;
    mouse_event(MOUSEEVENTF_WHEEL, 0, 0, fy, 0);
}







BYTE scan_code(DWORD pKey)
{
    const DWORD result = MapVirtualKey(pKey, MAPVK_VK_TO_VSC);
    return static_cast<BYTE>(result);
}

void game_press(int vk) {
    keybd_event(vk, scan_code(vk), 0, 0);
    keybd_event(vk, scan_code(vk), KEYEVENTF_KEYUP, 0);
}

void game_down(int vk) {
    keybd_event(vk, scan_code(vk), 0, 0);
}

void game_up(int vk) {
    keybd_event(vk, scan_code(vk), KEYEVENTF_KEYUP, 0);
}


int main()
{
    bool m = TRUE;
    int v = volume();
    setVolum(1);
    setVolum(v);
    string addr = "";


    while (TRUE) {
        int len;
        char recv_buf[10240] = { "\0" };//接收缓冲区

        SOCKET server;
        SOCKET client;

        SOCKADDR_IN server_addr;//使用结构体 SOCKADDR_IN 存储配置
        SOCKADDR_IN client_addr;

        WSADATA wsaData;
        WSAStartup(MAKEWORD(2, 2), &wsaData);

        server = socket(AF_INET, SOCK_STREAM, 0);

        memset(&server_addr, 0, sizeof(server_addr));
        server_addr.sin_family = AF_INET;
        server_addr.sin_port = htons(3333); //监听端口
        client_addr.sin_port = 4333;

        bind(server, (SOCKADDR*)&server_addr, sizeof(SOCKADDR));

        listen(server, 10);
        len = sizeof(SOCKADDR);

        client = accept(server, (SOCKADDR*)&client_addr, &len);


        struct sockaddr_in* sock = (struct sockaddr_in*)&client_addr;
        int port = ntohs(sock->sin_port);

        string addr2 = inet_ntoa(sock->sin_addr);
        cout << "from: " << addr2 << endl;




        recv(client, recv_buf, 10240, 0);
        cout << "recv_buf: " << string(recv_buf);
        if (string(recv_buf).find("IP::") != string::npos && addr == "") {
            char c = '\0';
            string r = string(recv_buf);
            r.erase(remove(r.begin(), r.end(), '\n'), r.end());
            r = r.replace(0, 4, "IP::", c);
            cout << "addr::" << r << endl;
            addr = r;
        }
        if (addr == "") {
            addr = addr2;
        }

        if (string(recv_buf) == "!GetVolume\n") {
            cout << "connected" << endl;
            send_volume(addr);
        }        
        if (string(recv_buf) == "!GetM\n") {
            cout << "!GetM" << endl;
            send_state(addr, m);
        }

        if (string(recv_buf) == "!Sub\n") {
            cout << "!Sub" << endl;
            keybd_event(174, 0, 0, 0);
            send_volume(addr);

        }

        if (string(recv_buf) == "!Add\n") {
            cout << "!Add" << endl;
            keybd_event(175, 0, 0, 0);
            send_volume(addr);
        }
        if (string(recv_buf) == "!Mute\n") {
            cout << "!Mute" << endl;
            if (m) {
                setVolum(-1);
                m = FALSE;
            }
            else {
                setVolum(-2);
                m = TRUE;
            }

            send_volume(addr);
            send_state(addr, m);

        }

        // ===============

        if (string(recv_buf) == "!Paused\n") {
            cout << "!Paused" << endl;

            keybd_event(179, 0, 0, 0);

            send_volume(addr);
            send_state(addr, m);

        }
        if (string(recv_buf) == "!Next\n") {
            cout << "!Next" << endl; // 下一首

            keybd_event(176, 0, 0, 0);

            send_volume(addr);
            send_state(addr, m);

        }
        if (string(recv_buf) == "!Previous\n") {
            cout << "!Previous" << endl;

            keybd_event(177, 0, 0, 0); // 上一首

            send_volume(addr);
            send_state(addr, m);

        }

        // ====================

        if (string(recv_buf) == "!Find\n") {
            cout << "!Find" << endl;
            Send(addr2, "OK!!");
        }


        // ===================

        
        // {"x":"-837.9492", "y":"-1380.8331"}
        if (string(recv_buf).find("x") != string::npos && string(recv_buf).find("y") != string::npos) {
            thread th_move(move_to, string(recv_buf));
            th_move.detach();
        }
        if (string(recv_buf).find("S::") != string::npos) {
            thread th_scroll(scroll_to, string(recv_buf));
            th_scroll.detach();
        }
        // =================

        if (string(recv_buf) == "!LClick\n") {
            cout << "!LClick" << endl;
            POINT p;
            GetCursorPos(&p);
            mouse_event(MOUSEEVENTF_LEFTDOWN | MOUSEEVENTF_LEFTUP, p.x, p.y, 0, 0);
        }
        if (string(recv_buf) == "!RClick\n") {
            cout << "!RClick" << endl;
            POINT p;
            GetCursorPos(&p);
            mouse_event(MOUSEEVENTF_RIGHTDOWN | MOUSEEVENTF_RIGHTUP, p.x, p.y, 0, 0);
        }
        if (string(recv_buf) == "!LongClick\n") {
            cout << "!LongClick" << endl;
            POINT p;
            GetCursorPos(&p);
            mouse_event(MOUSEEVENTF_LEFTDOWN, p.x, p.y, 0, 0);
        }
        if (string(recv_buf) == "!Up\n") {
            cout << "!Up" << endl;
            POINT p;
            GetCursorPos(&p);
            mouse_event(MOUSEEVENTF_LEFTUP, p.x, p.y, 0, 0);
        }

        // =================

        if (string(recv_buf) == "NumPad::1\n") {
            game_press(97);
        }
        if (string(recv_buf) == "NumPad::2\n") {
            game_press(98);
        }
        if (string(recv_buf) == "NumPad::3\n") {
            game_press(99);
        }
        if (string(recv_buf) == "NumPad::4\n") {
            game_press(100);
        }
        if (string(recv_buf) == "NumPad::5\n") {
            game_press(101);
        }
        if (string(recv_buf) == "NumPad::6\n") {
            game_press(102);
        }
        if (string(recv_buf) == "NumPad::7\n") {
            game_press(103);
        }
        if (string(recv_buf) == "NumPad::8\n") {
            game_press(104);
        }
        if (string(recv_buf) == "NumPad::9\n") {
            game_press(105);
        }
        if (string(recv_buf) == "NumPad::0\n") {
            game_press(96);
        }
        if (string(recv_buf) == "NumPad::Enter\n") {
            game_press(13);
        }
        
        // =================


        if (string(recv_buf) == "NumPad::1DOWN\n") {
            game_down(97);
        }
        if (string(recv_buf) == "NumPad::2DOWN\n") {
            game_down(98);
        }
        if (string(recv_buf) == "NumPad::3DOWN\n") {
            game_down(99);
        }
        if (string(recv_buf) == "NumPad::4DOWN\n") {
            game_down(100);
        }
        if (string(recv_buf) == "NumPad::5DOWN\n") {
            game_down(101);
        }
        if (string(recv_buf) == "NumPad::6DOWN\n") {
            game_down(102);
        }
        if (string(recv_buf) == "NumPad::7DOWN\n") {
            game_down(103);
        }
        if (string(recv_buf) == "NumPad::8DOWN\n") {
            game_down(104);
        }
        if (string(recv_buf) == "NumPad::9DOWN\n") {
            game_down(105);
        }
        if (string(recv_buf) == "NumPad::0DOWN\n") {
            game_down(96);
        }
        if (string(recv_buf) == "NumPad::EnterDOWN\n") {
            game_down(13);
        }
        
        if (string(recv_buf) == "NumPad::1UP\n") {
            game_up(97);
        }
        if (string(recv_buf) == "NumPad::2UP\n") {
            game_up(98);
        }
        if (string(recv_buf) == "NumPad::3UP\n") {
            game_up(99);
        }
        if (string(recv_buf) == "NumPad::4UP\n") {
            game_up(100);
        }
        if (string(recv_buf) == "NumPad::5UP\n") {
            game_up(101);
        }
        if (string(recv_buf) == "NumPad::6UP\n") {
            game_up(102);
        }
        if (string(recv_buf) == "NumPad::7UP\n") {
            game_up(103);
        }
        if (string(recv_buf) == "NumPad::8UP\n") {
            game_up(104);
        }
        if (string(recv_buf) == "NumPad::9UP\n") {
            game_up(105);
        }
        if (string(recv_buf) == "NumPad::0UP\n") {
            game_up(96);
        }
        if (string(recv_buf) == "NumPad::EnterUP\n") {
            game_up(13);
        }

        // =============================


        if (string(recv_buf) == "BackSpace\n") {
            game_press(8);
        }

        if (string(recv_buf) == "CtrlZ\n") {
            game_down(VK_CONTROL);
            game_press(90);
            game_up(VK_CONTROL);
        }

        // ==============

        if (string(recv_buf) == "!shutdown\n") {
            system("shutdown -s -t 0");
        }
        if (string(recv_buf) == "!sleep\n") {
            system("shutdown -h");
        }

        // ===============

        if (string(recv_buf) == "!HeartBeat\n") {
            Send(addr ,"!!HeartBeat");
        }

        WSACleanup();

    }

    


    return 0;
}



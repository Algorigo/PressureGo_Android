#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_algorigo_pressurego_PressureGo_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from Pressure Go Library";
    return env->NewStringUTF(hello.c_str());
}
#include "org_thoughtcrime_securesms_util_FileUtils.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <linux/memfd.h>
#include <syscall.h>

JNIEXPORT jint

JNICALL Java_org_thoughtcrime_securesms_util_FileUtils_createMemoryFileDescriptor
        (JNIEnv *env, jclass clazz, jstring jname) {
    const char *name = env->GetStringUTFChars(jname, NULL);

    int fd = syscall(SYS_memfd_create, name, MFD_CLOEXEC);

    env->ReleaseStringUTFChars(jname, name);

    return fd;
}

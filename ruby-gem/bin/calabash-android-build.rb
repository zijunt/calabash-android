require 'zip/zip'
def calabash_build(app)
  keystore = read_keystore_info()
  project_dir = Dir.pwd

  test_server_file_name = test_server_path(app)
  FileUtils.mkdir_p File.dirname(test_server_file_name) unless File.exist? File.dirname(test_server_file_name)

  unsigned_test_apk = File.join(File.dirname(__FILE__), '..', 'lib/calabash-android/lib/TestServer.apk')
  calabash_jar = File.join(File.dirname(__FILE__), '..', 'lib/calabash-android/lib/calabash.jar')
  robotium_jar = File.join(File.dirname(__FILE__), '..', 'test-server/instrumentation-backend/libs/robotium-solo-3.3.jar')
  all_android_platforms = Dir["#{ENV["ANDROID_HOME"].gsub("\\", "/")}/platforms/android-*"]
  android_platform = all_android_platforms.sort {|x,y| x.split("-").last.to_i <=> y.split("-").last.to_i}.last
  raise "No Android SDK found in #{ENV["ANDROID_HOME"].gsub("\\", "/")}/platforms/" unless android_platform
  android_jar =  "#{android_platform}/android.jar"
  has_plugins =  File.exists?(File.join(project_dir, "plugins"))
  if has_plugins
    cmd = "javac -cp #{calabash_jar}:#{robotium_jar}:#{android_jar} plugins/sh/calaba/instrumentationbackend/actions/*.java"
    log cmd
    raise "Could not compile plugins" system cmd
  end
  Dir.mktmpdir do |workspace_dir|
    Dir.chdir(workspace_dir) do
      FileUtils.cp(unsigned_test_apk, "TestServer_aapt.apk")
      FileUtils.cp(File.join(File.dirname(__FILE__), '..', 'test-server/AndroidManifest.xml'), "AndroidManifest.xml")

      unless system %Q{ruby -pi.bak -e "gsub(/#targetPackage#/, '#{package_name(app)}')" AndroidManifest.xml}
        raise "Could not replace package name in manifest"
      end


      plugin_path = File.join(project_dir, "plugins") if has_plugins
      puts `dx --dex --verbose --output classes.dex #{calabash_jar} #{plugin_path} #{robotium_jar}`

      cmd = "apkbuilder TestServer.apk -u -z TestServer_aapt.apk -f classes.dex"
      puts cmd
      puts `#{cmd}`



      unless system %Q{"#{ENV["ANDROID_HOME"]}/platform-tools/aapt" package -M AndroidManifest.xml  -I "#{android_jar}" -F dummy.apk}
        raise "Could not create dummy.apk"
      end

      Zip::ZipFile.new("dummy.apk").extract("AndroidManifest.xml","customAndroidManifest.xml")
      Zip::ZipFile.open("TestServer.apk") do |zip_file|
        zip_file.add("AndroidManifest.xml", "customAndroidManifest.xml")  
      end
    end
    if is_windows?
      jarsigner_path = "\"#{ENV["JAVA_HOME"]}/bin/jarsigner.exe\""
    else
      jarsigner_path = "jarsigner"
    end

    cmd = "#{jarsigner_path} -sigalg MD5withRSA -digestalg SHA1 -signedjar #{test_server_file_name} -storepass #{keystore["keystore_password"]} -keystore \"#{File.expand_path keystore["keystore_location"]}\" #{workspace_dir}/TestServer.apk #{keystore["keystore_alias"]}"           
    unless system(cmd)
      puts "jarsigner command: #{cmd}"
      raise "Could not sign test server"
    end
  end
  puts "Done signing the test server. Moved it to #{test_server_file_name}"
end


def read_keystore_info
  if File.exist? ".calabash_settings"
    JSON.parse(IO.read(".calabash_settings"))
  else
    {
    "keystore_location" => "#{ENV["HOME"]}/.android/debug.keystore",
    "keystore_password" => "android",
    "keystore_alias" => "androiddebugkey",
    "keystore_alias_password" => "android"
    }
  end
end
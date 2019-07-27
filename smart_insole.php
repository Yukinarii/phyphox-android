<?php

$con=mysqli_connect('localhost','user','!QAZ2wsx');
if (mysqli_connect_errno())
{
    echo "Failed to connect to MySQL: " . mysqli_connect_error();
    // need to exit the script, if there is an error
    exit();
}
mysqli_select_db($con, "smart_insole"); // The name of the database

$UID=$_POST['UID'];
$Table=$_POST['Sensor'];
if ($Table == "User")
{
    //if (mysqli_query($con, "DESCRIBE '{$Table}'")) { // if table exists
        $Name=$_POST['Name'];
        $Age=$_POST['Age'];
        $Gender=$_POST['Gender'];
        $Height=$_POST['Height'];
        $Weight=$_POST['Weight'];
        $Nationality=$_POST['Nationality'];
        $Job=$_POST['Job'];

        mysqli_query($con, "insert into ".$Table."(UID, Name, Age, Gender, Height, Weight, Nationality, Job) ".
                       "values('{$UID}', '{$Name}', '{$Age}', '{$Gender}', '{$Height}', '{$Weight}', '{$Nationality}', '{$Job}')");
    //}
    /*else {
        mysqli_query($con, "create table ".$Table."(UID varchar(32) NOT NULL, Name varchar(32), Age int(16), Gender varchar(16),".
                           "Height float(16), Weight float(16), Nationality varchar(16), Job varchar(16), PRIMARY KEY (UID))");
    }*/
}
else if ($Table == "location") {
    //if (mysqli_query($con, "DESCRIBE '{$Table}'")) {
        $dataLat=$_POST['dataLat'];
        $dataLon=$_POST['dataLon'];
        $dataZ=$_POST['dataZ'];
        $Velocity=$_POST['dataV'];
        $timestamp=$_POST['timestamp'];
        $dataAccuracy=$_POST['dataAccuracy'];
        $dataZAccuracy=$_POST['dataZAccuracy'];
        $dataSatellite=$_POST['dataSatellites'];

        mysqli_query($con, "insert into ".$Table."(UID, Latitude, Longitude, dataZ, Velocity, timestamp, dataAccuracy, dataZaccuracy, dataSatellite) ".
                        "values('{$UID}', '{$dataLat}', '{$dataLon}', '{$dataZ}', '{$Velocity}', '{$timestamp}', '{$dataAccuracy}', '{$dataZAccuracy}', '{$dataSatellite}')");
    //}
    /*else {
        mysqli_query($con, "create table ".$Table.
        "(UID varchar(32) NOT NULL, Latitude float(32), Longitude float(32), dataZ float(32), Velocity float(32), timestamp float(32), dataAccuracy float(32),".
         "dataZAccuracy float(32), dataSatellite float(32))");
    }*/
}
else
{
    echo "Sensor data";
    //if (mysqli_query($con, "DESCRIBE '{$Table}'")) { // if table exists
        $dataX=$_POST['dataX'];
        $dataY=$_POST['dataY'];
        $dataZ=$_POST['dataZ'];
        $dataT=$_POST['timestamp'];
        $dataAbs=$_POST['dataAbs'];
        $dataAccuracy=$_POST['dataAccuracy'];
    
        mysqli_query($con, "insert into ".$Table."(UID, dataX, dataY, dataZ, timestamp, dataAbs, dataAccuracy) ".
                           "values('{$UID}', '{$dataX}', '{$dataY}', '{$dataZ}', '{$dataT}', '{$dataAbs}', '{$dataAccuracy}')");
    //}
    /*else {
        mysqli_query($con, "create table ".$Table.
        "(UID varchar(32) NOT NULL, dataX float(32), dataY float(32), dataZ float(32), timestamp float(32), dataAccuracy float(32))");
    }*/
   
}

//mysql_close();
?>

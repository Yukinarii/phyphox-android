<?php

$con=mysqli_connect('localhost','user','!QAZ2wsx');
if (mysqli_connect_errno())
{
    echo "Failed to connect to MySQL: " . mysqli_connect_error();
    // need to exit the script, if there is an error
    exit();
}
mysqli_select_db($con, "smart_insole"); // The name of the database
echo "login to database\n";
$jsonArray=file_get_contents('php://input');
$jsonData=json_decode($jsonArray, true);
echo "get json file\n";
mysqli_query($con, "BEGIN");
foreach ($jsonData as $row) {
    $UID=$row["UID"];
    $dataX=$row["dataX"];
    $dataY=$row["dataY"];
    $dataZ=$row["dataZ"];
    $dataT=$row["timestamp"];
    $dataAbs=$row["dataAbs"];
    $dataAccuracy=$row["dataAccuracy"];
    $Table=$row["Sensor"];
    mysqli_query($con, "insert delayed into ".$Table."(UID, dataX, dataY, dataZ, timestamp, dataAbs, dataAccuracy)".
                           "values('{$UID}', '{$dataX}', '{$dataY}', '{$dataZ}', '{$dataT}', '{$dataAbs}', '{$dataAccuracy}')");    
}
mysqli_query($con, "COMMIT");
?>

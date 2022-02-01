<?php
/**
 * A Script for accepting URL client requests, accessing MySQL DB, and sending
 * replies. To be used for City Networks bread delivery days.
 *
 * request:  regioncode:identifier:RequestAvailableRoutes::
 * response: RequestAvailableRoutes:pk1~pk2:routename1~routename2::
 *
 * request:  regioncode:identifier:RequestRoutes:pk1~pk2,...:
 * response: RequestRoutes:accepted1pk~accepted1name:declined1pk~name::
 *
 * request:  regioncode:identifier:RequestRouteInfo:routePK:
 * response: RequestRouteInfo:routePK:church1info1~church2info2~...:routeName:
 * churchInfo Format: pk|name|time|address|isDelivered
 *
 * request:  regioncode:identifier:RequestAssignedRoutes::
 * response: RequestAssignedRoutes:pk1~pk2:routename1~routename2::
 *
 * request:  regioncode:identifier:ConfirmDeliveries:route1:pk1~true
 * response: ConfirmDeliveries:pk1~true:::
 *
 *
 * * request:  regioncode:identifier:RelinquishRoutes:pk1~pk2,...:
 * response: RelinquishRoutes:route1~route2~route3:::
 *
 * - other possible responses
 *
 * response: Error:errorMessage:::
 *
 * - Notes
 *
 * 'regioncode' is the unique code per group of routes. This will be unique
 *  per region.
 *
 * 'identifier' field is used for authentication, and must be a valid
 * MAC address. (00-00-00-00-00-00)
 *
 * space charcters within the query string may be encoded as '%20'
 *
 *  - Delimiters: ':' '~' '|' '$'
 */

/*---- PREP ----*/
require '../../db_access.php';
$mysqli = getMysqli();
if ($mysqli == -1)
    sendReply('Error', 'failed loading db connection or regionPK');

error_reporting(E_ALL & ~E_STRICT & ~E_NOTICE);

// Import query, blow it up, name the rubble
$params = explode(':', $_SERVER['QUERY_STRING']);
define('REGIONCODE', $params[0]);
define('IDENTIFIER', $params[1]);
define('REQUEST', $params[2]);
define('INPUT_VAR_1', str_replace('%20', ' ', $params[3]));
define('INPUT_VAR_2', str_replace('%20', ' ', $params[4]));

/*---- SECURITY ----*/
// Ahh, gotta love that fool-proof security
// Validate IDENTIFIER valid MAC address
if (preg_match('/([a-fA-F0-9]{2}[-]?){6}/', IDENTIFIER) != 1)
    sendReply('Error', 'invalid MAC address');

// Validate REGIONCODE is not null
if (REGIONCODE == null)
    sendReply('Error', 'REGIONCODE does not exist');

// Validate REGIONCODE exists in DB
$query = "SELECT name FROM region WHERE code=" . REGIONCODE;
$result = $mysqli->query($query);
if ($result->num_rows === 0)
    sendReply('Error', 'REGIONCODE does not exist');

/*---- FUNCTIONALITY ----*/
// The heart of the thing, calls function according to value of REQUEST
switch (REQUEST) {
    case 'RequestAvailableRoutes':
        requestAvailableRoutes();
        break;
    case 'RequestRoutes':
        requestRoutes();
        break;
    case 'RequestRouteInfo':
        requestRouteInfo();
        break;
    case 'RequestAssignedRoutes':
        requestAssignedRoutes();
        break;
    case 'ConfirmDeliveries':
        confirmDeliveries();
        break;
    case 'RelinquishRoutes':
        relinquishRoutes();
        break;
    default:
        sendReply('Error', 'request not recognised');
}

/**
 * Returns list of routes in region with no assignedDriver
 * request:  regioncode:identifier:RequestAvailableRoutes::
 * response: RequestAvailableRoutes:pk1~pk2:routename1~routename2::
 */
function requestAvailableRoutes() {
    global $mysqli;

    $query = <<<MYSQL
SELECT ro.__pk_id, ro.name
FROM region AS re, route AS ro 
WHERE re.code=? AND re.__pk_id=ro._fk_region AND (ro.driver_mac='' OR ro.driver_mac IS NULL);
MYSQL;

    $stmt = $mysqli->prepare($query);
    $stmt->bind_param("i", $re = REGIONCODE);
    $stmt->execute();
    $stmt->bind_result($pk, $na);

    $pks = [];
    $names = [];
    while ($stmt->fetch()) {
        $pks[] = $pk;
        $names[] = $na;
    }

    $pksString = implode('~', $pks);
    $namesString = implode('~', $names);
    sendReply(REQUEST, $pksString, $namesString);
}

/**
 * Uses INPUT_VAR_1 as input array of requested routes.
 * If route has no driver,  set driver = IDENTIFIER. Return accepted/declined in
 * formatted response.
 * request:  regioncode:identifier:RequestRoutes:pk1~pk2,...:
 * response: RequestRoutes:accepted1pk~accepted1name:declined1pk~name::
 */
function requestRoutes() {
    global $mysqli;

    $query = "UPDATE route, region SET route.driver_mac = '" . IDENTIFIER . "' 
        WHERE region.__pk_id = route._fk_region AND region.CODE = '" . REGIONCODE . "' 
        AND route.__pk_id = ? AND (route.driver_mac = '' OR route.driver_mac IS NULL)";

    $pks = explode('~', INPUT_VAR_1);
    $accepted = [];
    $declined = [];

    $stmt = $mysqli->prepare($query);
    foreach ($pks as $pk) {
        $stmt->bind_param("s", $pk);
        $stmt->execute();
        if ($stmt->affected_rows == 0) {
            $declined[] = $pk;
        } else {
            $accepted[] = $pk;
        }
    }

    $aString = implode('~', $accepted);
    $dString = implode('~', $declined);
    sendReply(REQUEST, $aString, $dString);
}

/**
 * Return churchName, deliveryTime, address, isDelivered for churches in
 * given route
 * request:  regioncode:identifier:RequestRouteInfo:routePK:
 * response: RequestRouteInfo:routePK:church1info1~church2info2~...:routeName:
 * churchInfo Format: pk|name|time|address|isDelivered
 */
function requestRouteInfo() {
    global $mysqli;

    $query = <<<MYSQL
SELECT __pk_id,name,bread_time,bread_add_line_1,bread_add_line_2
  ,bread_add_suburb,bread_add_state,bread_add_post_code,bread_delivered
FROM church WHERE bread_fk_route=? ORDER BY bread_time
MYSQL;
    $stmt = $mysqli->prepare($query);
    $stmt->bind_param("i", $iv1 = INPUT_VAR_1);
    $stmt->execute();
    $stmt->bind_result($pk, $nm, $tm, $ad1, $ad2, $sb, $st, $pc, $dv);

    $infoArray = [];
    while ($stmt->fetch()) {
        $tm = str_replace(':', '%58', $tm);
        $add = $ad1 . " " . $ad2 . " " . $sb . " " . $st . " " . $pc;
        $dv = ($dv == '0') ? "not delivered" : "delivered";
        $infoArray[] = implode('|', [$pk, $nm, $tm, $add, $dv]);
    }
    $infoString = implode('~', $infoArray);

    $query = "SELECT name FROM route WHERE __pk_id = ?";
    $stmt = $mysqli->prepare($query);
    $stmt->bind_param("i", $iv1 = INPUT_VAR_1);
    $stmt->execute();
    $stmt->bind_result($routeName);
    $stmt->fetch();

    sendReply(REQUEST, INPUT_VAR_1, $infoString, $routeName);
}

/**
 * Return list of routes where driver = IDENTIFIER
 * request:  regioncode:identifier:RequestAssignedRoutes::
 * response: RequestAssignedRoutes:pk1~pk2:routename1~routename2::
 */
function requestAssignedRoutes() {
    global $mysqli;

    $query = "SELECT __pk_id,name FROM route WHERE driver_mac=?";
    $stmt = $mysqli->prepare($query);
    $stmt->bind_param("s", $mac = IDENTIFIER);
    $stmt->execute();
    $stmt->bind_result($pk, $nm);

    $pks = [];
    $nms = [];
    while ($stmt->fetch()) {
        $pks[] = $pk;
        $nms[] = $nm;
    }

    $pksString = implode('~', $pks);
    $nmsString = implode('~', $nms);
    sendReply(REQUEST, $pksString, $nmsString);
}

/**
 * Allows the user to confirm delivery of churches
 * request:  regioncode:identifier:ConfirmDeliveries:route1:pk1~true
 * response: ConfirmDeliveries:route1:pk1::
 */
function confirmDeliveries() {
    global $mysqli;

    $iv2 = INPUT_VAR_2;
    $iv2 = str_replace("not delivered", "0", $iv2);
    $iv2 = str_replace("delivered", "1", $iv2);
    $infoArray = explode('~', $iv2);

    $query = "UPDATE church SET bread_delivered=? WHERE bread_fk_route=? AND __pk_id=?";
    $stmt = $mysqli->prepare($query);
    $stmt->bind_param("sii", $infoArray[1], $pk = INPUT_VAR_1, $infoArray[0]);
    $stmt->execute();

    sendReply(REQUEST, INPUT_VAR_1, $infoArray[0]);
}

/**
 * Changes the given route's driver to NULL
 * request:  regioncode:identifier:RelinquishRoutes:pk1~pk2~...:
 * response: RelinquishRoutes:route1~route2~route3:::
 */
function relinquishRoutes() { // TODO fix
    global $mysqli;

    $query = "UPDATE route SET driver_mac = NULL WHERE __pk_id=? AND driver_mac=?";
    $stmt = $mysqli->prepare($query);
    $stmt->bind_param("ss", $pk, $mac = IDENTIFIER);

    $pks = explode('~', INPUT_VAR_1);
    $relinquished = [];
    foreach ($pks as $pk) {
        $stmt->execute();
        if (!$stmt->affected_rows == 0) {
            $relinquished[] = $pk;
        }
    }

    $relinquishedString = implode('~', $relinquished);
    sendReply(REQUEST, $relinquishedString);
}

/**
 * @param string $param0
 * @param string $param1
 * @param string $param2
 * @param string $param3
 * @param string $param4
 */
function sendReply($param0 = '', $param1 = '', $param2 = '', $param3 = '',
                   $param4 = '') {

    $param0 = str_replace(':', '%58', $param0);
    $param1 = str_replace(':', '%58', $param1);
    $param2 = str_replace(':', '%58', $param2);
    $param3 = str_replace(':', '%58', $param3);
    $param4 = str_replace(':', '%58', $param4);

    echo "$param0:$param1:$param2:$param3:$param4";
    exit();
}

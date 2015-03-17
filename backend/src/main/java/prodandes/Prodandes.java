/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package prodandes;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Jonathan
 */
@Path("/Servicios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
/**
 *
 * @author jf.molano1587
 */
public class Prodandes {

    public Connection con;

    // -------------------------------------------------
    // Requerimientos Funcionales
    // -------------------------------------------------
    @POST
    @Path("/registrarPedido")
    public JSONObject registrarPedido(JSONObject jO) throws Exception {
        try {

            String resp = "";

            abrirConexion();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jO.get("fechaEsperada").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String sFecha = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            System.out.println("Fecha: " + sFecha);

            String nombreProducto = jO.get("nombre").toString();
            int cantidad = (int) jO.get("cantidad");
            int id_cliente = (int) jO.get("id_cliente");

            Calendar c = new GregorianCalendar();
            String fechaSolicitud = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 1) + "-" + c.get(GregorianCalendar.YEAR);

            System.out.println("FEcha actual " + fechaSolicitud);
            String sql = "select max (id) as MAXIMO from PEDIDO_PRODUCTO";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            int id_pedido = -1;
            if (rs.next()) {
                id_pedido = rs.getInt("MAXIMO") + 1;

                //Crear pedido nuevo
                sql = "INSERT INTO PEDIDO_PRODUCTO (id,FECHA_ESPERADA_ENTREGA,Estado,cantidad_producto"
                        + ",id_cliente,fecha_solicitud) VALUES (" + id_pedido + ",TO_DATE"
                        + "('" + sFecha + "','DD-MM-YYYY'),'Espera'," + cantidad + ","
                        + id_cliente + " ,TO_DATE('" + fechaSolicitud + "','DD-MM-YYYY'))";

                Statement st2 = con.createStatement();

                st2.executeUpdate(sql);

                st2.close();
            }
            st.close();

            if (id_pedido == -1) {
                throw new Exception("Error asignando ID del pedido");
            }
            int productosReservados = reservarProductoBodega(nombreProducto, cantidad, id_pedido);

            if (productosReservados == cantidad) {

                //Modificar fecha entrega
                Statement st3 = con.createStatement();
                sql = "update PEDIDO_PRODUCTO set FECHA_ENTREGA=TO_DATE('" + sFecha + "','DD-MM-YYYY'),"
                        + "ESTADO='En Bodega'"
                        + "where id=" + id_pedido;
                st3.executeUpdate(sql);
                st3.close();
                resp = "Se ha registrado su pedido, la fecha de entrega es " + sFecha;
            } else {

                // Verificar que la cantidad disminuye dependiendo de cuantos productos ya están en bodega
                cantidad = cantidad - productosReservados;

                //Reservar recursos(materias primas) o pedir suministros
                int numProductosPotencial = Integer.MAX_VALUE;

                System.out.println("Nombre producto: " + nombreProducto);
                //Averiguar Componentes en bodega
                sql = "select * from COMPONENTES_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                Statement st3 = con.createStatement();
                rs = st3.executeQuery(sql);

                while (rs.next()) {

                    String id_componente = rs.getString("id_componente");
                    int cantidad_unidades = rs.getInt("cantidad_unidades");

                    int numComponentes = cantidadComponentesBodega(id_componente);
                    if (numComponentes >= cantidad_unidades) {

                        int alcanzanComponentes = numComponentes / cantidad_unidades;
                        numProductosPotencial = Math.min(alcanzanComponentes, numProductosPotencial);
                    }
                }

                st3.close();

                //Averiguar Materias Primas en bodega
                sql = "select * from MATERIAS_PRIMAS_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                st3 = con.createStatement();
                rs = st3.executeQuery(sql);

                while (rs.next()) {

                    String id_materia = rs.getString("id_materia_prima");
                    int cantidad_unidades = rs.getInt("cantidad_unidades");

                    int numMateriasBodega = cantidadMateriasPrimasBodega(id_materia);
                    if (numMateriasBodega >= cantidad_unidades) {

                        int alcanzanMaterias = numMateriasBodega / cantidad_unidades;
                        numProductosPotencial = Math.min(alcanzanMaterias, numProductosPotencial);
                    }
                }

                st3.close();

                System.out.println("Numero productos se pueden hacer con bodega " + numProductosPotencial);

                if (numProductosPotencial != Integer.MAX_VALUE
                        && numProductosPotencial >= cantidad) {

                    //Reservar componentes
                    sql = "select * from COMPONENTES_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                    st3 = con.createStatement();
                    rs = st3.executeQuery(sql);

                    while (rs.next()) {

                        String id_componente = rs.getString("id_componente");
                        int cantidad_unidades = rs.getInt("cantidad_unidades");
                        reservarComponenteBodega(id_componente, cantidad * cantidad_unidades, id_pedido);
                    }
                    st3.close();

                    // Reservar materias primas
                    sql = "select * from MATERIAS_PRIMAS_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                    st3 = con.createStatement();
                    rs = st3.executeQuery(sql);

                    while (rs.next()) {

                        String id_materia = rs.getString("id_materia_prima");
                        int cantidad_unidades = rs.getInt("cantidad_unidades");
                        reservarMateriaPrimaBodega(id_materia, cantidad * cantidad_unidades, id_pedido);
                    }

                    st3.close();

                    //Falta fecha esperada
                    crearItemsReservadosPedido(nombreProducto, id_pedido, cantidad);
                    resp = "Se registro su pedido, la fecha esperada de entrega es ";
                } else {
                    //Poner el pedido en estad ESPERA
                    st3 = con.createStatement();
                    sql = "update PEDIDO_PRODUCTO set ESTADO='Espera' where id=" + id_pedido;
                    st3.executeUpdate(sql);
                    st3.close();

                    resp = "En estos momentos no contamos con los suministros suficientes para proceder"
                            + "con su pedido, vamos a poner su pedido en estado de espera.";
                }

            }
            cerrarConexion();
            //return resp;
            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", resp);
            return jRespuesta;
        } catch (Exception e) {
            e.printStackTrace();
            cerrarConexion();
            //return "error";
            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "error");
            return jRespuesta;
        }
    }

    @POST
    @Path("/registrarEntregaPedidoProductosCliente")
    /**
     * Registrar que los productos del pedido ya se entregaron
     *
     * @param id_pedido
     */
    public void registrarEntregaPedidoProductosCliente(JSONObject jP) throws Exception {

        int id_pedido = (int) jP.get("id_pedido");
        abrirConexion();
        String query = "select cantidad_producto from PEDIDO_PRODUCTO WHERE ID=" + id_pedido;

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int cantidad = -1;
        if (rs.next()) {
            cantidad = rs.getInt("cantidad_producto");
        }
        if (cantidad == -1) {
            throw new Exception("No se encontró el pedido");
        }

        st.close();
        query = "select * from ITEM where ID_PEDIDO=" + id_pedido + " and ESTADO='Reservado'";
        st = con.createStatement();
        rs = st.executeQuery(query);
        int cantidadProductosProducidos = 0;
        while (rs.next()) {

            cantidadProductosProducidos++;
        }
        st.close();

        if (cantidadProductosProducidos < cantidad) {

            throw new Exception("No se han terminado de producir todos los productos");
        } else {
            st = con.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {

                int id = rs.getInt("ID");
                String sql2 = "update ITEM set ESTADO='Entregado' where ID = " + id;

                Statement st2 = con.createStatement();
                st2.executeUpdate(sql2);
                st2.close();
            }

            st.close();

            query = "UPDATE PEDIDO_PRODUCTO SET ESTADO='Entregado' WHERE ID=" + id_pedido;
            st = con.createStatement();
            st.executeUpdate(query);
            st.close();
        }
        cerrarConexion();
    }

    @POST
    @Path("/consultarProductos")
    public JSONArray consultarProductos(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();

        String criterio = jP.get("Criterio").toString();
        if (criterio.equalsIgnoreCase("Rango")) {

            int rango1 = (int) jP.get("Rango1");
            int rango2 = (int) jP.get("Rango2");

            String sql = "Select * from (Select Producto.Nombre as nombreProducto,count(*) as cantidadInventario "
                    + "from (Item inner join Producto on "
                    + "Producto.nombre=Item.nombre_Producto)"
                    + "WHERE Item.Estado ='En Bodega' GROUP BY Producto.nombre) where cantidadInventario>" + rango1
                    + " AND cantidadInventario<" + rango2;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String nomProd = rs.getString("nombreProducto");

                sql = "Select * from ITEM where nombre_producto='" + nomProd + "' AND ESTADO='En Bodega'";

                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs2.getInt("id"));
                    jObject.put("ESTADO", rs2.getString("ESTADO"));
                    jObject.put("NOMBRE_PRODUCTO", rs2.getString("NOMBRE_PRODUCTO"));
                    jObject.put("ETAPA", rs2.getInt("ETAPA"));
                    jObject.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jObject);
                }

                st2.close();
            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Etapa")) {

            int num_etapa = (int) jP.get("Etapa");

            String sql = "select * from Item where etapa=" + num_etapa;
            Statement st2 = con.createStatement();
            ResultSet rs2 = st2.executeQuery(sql);

            while (rs2.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("id", rs2.getInt("id"));
                jObject.put("ESTADO", rs2.getString("ESTADO"));
                jObject.put("NOMBRE_PRODUCTO", rs2.getString("NOMBRE_PRODUCTO"));
                jObject.put("ETAPA", rs2.getInt("ETAPA"));
                jObject.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                jArray.add(jObject);
            }

            st2.close();

        } else if (criterio.equalsIgnoreCase("Fecha solicitud")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            System.out.println("Fecha "+fechaS);
            String sql = "select * from PEDIDO_PRODUCTO where fecha_solicitud = "
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("NOMBRE_PRODUCTO", rs2.getString("NOMBRE_PRODUCTO"));
                    jO.put("ETAPA", rs2.getInt("ETAPA"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha entrega")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_PRODUCTO where fecha_entrega = "
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("NOMBRE_PRODUCTO", rs2.getString("NOMBRE_PRODUCTO"));
                    jO.put("ETAPA", rs2.getInt("ETAPA"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        }
        cerrarConexion();
        return jArray;
    }

    @POST
    @Path("/consultarMateriasPrimas")
    public JSONArray consultarMateriasPrimas(JSONObject jP) throws Exception {

        System.out.println("LLEGO");
        System.out.println("PArametro " + jP.toJSONString());
        JSONArray jArray = new JSONArray();
        abrirConexion();
        String criterio = jP.get("Criterio").toString();

        System.out.println("Criterio " + criterio);
        if (criterio.equalsIgnoreCase("Rango")) {

            int rango1 = (int) jP.get("Rango1");
            int rango2 = (int) jP.get("Rango2");

            System.out.println("Rango1" + rango1);
            System.out.println("Rango2" + rango2);
            String sql = "Select * from (Select Materia_Prima.Nombre as nombreMateria,count(*) as "
                    + "cantidadInventario "
                    + "from (Materia_Prima_Item inner join Materia_Prima on "
                    + "Materia_Prima.nombre=Materia_Prima_Item.materia)"
                    + "WHERE Materia_Prima_Item.Estado ='En Bodega' "
                    + "GROUP BY Materia_Prima.nombre) where cantidadInventario>" + rango1
                    + " AND cantidadInventario<" + rango2;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String materia = rs.getString("nombreMateria");

                System.out.println("Materia " + materia);
                sql = "Select * from Materia_Prima_Item where materia='" + materia + "' "
                        + "AND Estado='En Bodega'";

                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("MATERIA", rs2.getString("MATERIA"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));

                    jArray.add(jO);
                }

                st2.close();
            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha solicitud")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            
            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH)+ "-" + (cEsp.get(GregorianCalendar.MONTH)+1)+"-" 
                    + cEsp.get(GregorianCalendar.YEAR);

            System.out.println("FEcha pedido "+fechaS);
            
            String sql = "select * from PEDIDO_MATERIA_PRIMA "
                    + "where FECHA_PEDIDO="
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                System.out.println("id_pedido "+id_pedido);
                sql = "select * from MATERIA_PRIMA_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("MATERIA", rs2.getString("MATERIA"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha entrega")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_MATERIA_PRIMA where fecha_entrega="
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from MATERIA_PRIMA_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("MATERIA", rs2.getString("MATERIA"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Tipo_material")) {

            String tipo = jP.get("Tipo_material").toString();

            System.out.println("TIPO " + tipo);
            String sql = "select * from MATERIA_PRIMA where TIPO = '" + tipo + "'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String nombreMateria = rs.getString("nombre");
                System.out.println("Nombre materia " + nombreMateria);
                sql = "select * from MATERIA_PRIMA_ITEM where materia = '" + nombreMateria + "'";
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("MATERIA", rs2.getString("MATERIA"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);

                }
            }
        }
        cerrarConexion();
        return jArray;
    }

    @POST
    @Path("/consultarComponentes")
    public JSONArray consultarComponentes(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();

        String criterio = jP.get("Criterio").toString();
        if (criterio.equalsIgnoreCase("Rango")) {

            int rango1 = (int) jP.get("Rango1");
            int rango2 = (int) jP.get("Rango2");

            String sql = "Select * from (Select Componente.Nombre as nombreComponente,count(*) as "
                    + "cantidadInventario "
                    + "from (Componente_Item inner join Componente on "
                    + "Componente.nombre=Componente_Item.componente)"
                    + "WHERE Componente_Item.Estado ='En Bodega' "
                    + "GROUP BY Componente.nombre) where cantidadInventario>" + rango1
                    + " AND cantidadInventario<" + rango2;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String componente = rs.getString("nombreComponente");

                sql = "Select * from componente_item where componente='" + componente + "'"
                        + "AND Estado ='En Bodega'";

                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("COMPONENTE", rs2.getString("COMPONENTE"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));

                    jArray.add(jO);
                }

                st2.close();
            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha solicitud")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_COMPONENTE where fecha_pedido = "
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from COMPONENTE_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("COMPONENTE", rs2.getString("COMPONENTE"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha entrega")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_componente where fecha_entrega = "
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from componente_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("COMPONENTE", rs2.getString("COMPONENTE"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Tipo_material")) {

            String tipo = jP.get("Tipo_material").toString();

            String sql = "select * from componente where TIPO = '" + tipo + "'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String componente = rs.getString("nombre");
                sql = "select * from componente_ITEM where componente = '" + componente + "'";
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id", rs2.getInt("id"));
                    jO.put("ESTADO", rs2.getString("ESTADO"));
                    jO.put("COMPONENTE", rs2.getString("COMPONENTE"));
                    jO.put("ID_PEDIDO", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);

                }
            }
        }
        cerrarConexion();
        return jArray;
    }

    // -------------------------------------------------
    // Metodos Adicionales
    // -------------------------------------------------
    public void crearItemsReservadosPedido(String nombreProducto, int id_pedido, int cantidad) throws Exception {

        for (int i = 0; i < cantidad; i++) {
            String sql = "select max (id) as MAXIMO from ITEM";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int id_item = -1;
            if (rs.next()) {
                id_item = rs.getInt("MAXIMO") + 1;

                //Crear pedido nuevo
                sql = "INSERT INTO ITEM (id,ESTADO,NOMBRE_PRODUCTO,ID_PEDIDO,ETAPA)"
                        + " VALUES (" + id_item + ",'Pre Produccion','" + nombreProducto + "',"
                        + id_pedido + ",0)";

                Statement st2 = con.createStatement();

                st2.executeUpdate(sql);

                st2.close();
            }
            st.close();
        }

    }

    @POST
    @Path("/cantidadProductoEnBodega")
    public int cantidadProductoEnBodega(String nombre) throws Exception {
        System.out.println("Entrada parámetro cantidadProductoEnBodega");
        System.out.println(nombre);

        String query = "select count(*) as cuenta from ITEM where NOMBRE_PRODUCTO='" + nombre + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int resp = 0;
        if (rs.next()) {
            resp = rs.getInt("cuenta");
        }
        st.close();

        System.out.println("Return cantidadProductoEnBodega: " + resp);
        return resp;
    }

    @POST
    @Path("/cantidadMateriasPrimasBodega")
    public int cantidadMateriasPrimasBodega(String materia) throws Exception {
        System.out.println("Entrada parámetro cantidadMateriasPrimasBodega");
        System.out.println(materia);

        String query = "select count(*) as cuenta from MATERIA_PRIMA_ITEM "
                + "where MATERIA='" + materia + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int resp = 0;
        if (rs.next()) {
            resp = rs.getInt("cuenta");
        }
        st.close();

        System.out.println("Return cantidadMateriasPrimasBodega: " + resp);
        return resp;
    }

    @POST
    @Path("/cantidadComponentesBodega")
    public int cantidadComponentesBodega(String componente) throws Exception {
        System.out.println("Entrada parámetro cantidadComponentesBodega");
        System.out.println(componente);

        String query = "select count(*) as cuenta from COMPONENTE_ITEM "
                + "where COMPONENTE='" + componente + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int resp = 0;
        if (rs.next()) {
            resp = rs.getInt("cuenta");
        }
        st.close();

        System.out.println("Return cantidadComponentesBodega: " + resp);
        return resp;
    }

    public void abrirConexion() throws Exception {

        con = null;
        Class.forName("oracle.jdbc.driver.OracleDriver");
        con = DriverManager.getConnection("jdbc:oracle:thin:@157.253.238.224:1531:prod", "ISIS2304271510", "rproxyquark");

    }

    public void cerrarConexion() throws Exception {
        if (con != null) {
            con.close();
            con = null;
        }
    }

    /**
     * Se reserva la cantidad de ese producto que está en bodega, y se pasa a
     * estado reservado y se asocia los items con el pedido
     *
     * @param nombreProducto
     * @param cantidad
     * @param id_pedido
     * @throws Exception
     */
    public int reservarProductoBodega(String nombreProducto, int cantidad, int id_pedido) throws Exception {

        String query = "select * from ITEM where NOMBRE_PRODUCTO='" + nombreProducto + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);

        int i;
        for (i = 0; i < cantidad && rs.next(); i++) {

            int id = rs.getInt("ID");
            String sql2 = "update ITEM set ESTADO='Reservado',ID_PEDIDO=" + id_pedido + " where ID = " + id;

            Statement st2 = con.createStatement();
            st2.executeUpdate(sql2);
            st2.close();
        }

        st.close();
        return i;
    }

    public int reservarComponenteBodega(String id_componente, int cantidad_unidades, int id_pedido)
            throws Exception {

        System.out.println("reservarComponenteBodega " + cantidad_unidades);
        String query = "select * from COMPONENTE_ITEM where componente='" + id_componente
                + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);

        int i;
        for (i = 0; i < cantidad_unidades && rs.next(); i++) {

            int id = rs.getInt("id");
            String sql2 = "update COMPONENTE_ITEM set ESTADO='Reservado',ID_PEDIDO=" + id_pedido
                    + " where id =" + id;

            Statement st2 = con.createStatement();
            st2.executeUpdate(sql2);
            st2.close();
        }

        st.close();
        return i;

    }

    public int reservarMateriaPrimaBodega(String id_materia, int cantidad_unidades, int id_pedido)
            throws Exception {

        System.out.println("reservarComponenteBodega " + cantidad_unidades);

        String query = "select * from MATERIA_PRIMA_ITEM where materia='" + id_materia
                + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);

        int i;
        for (i = 0; i < cantidad_unidades && rs.next(); i++) {

            int id = rs.getInt("id");
            String sql2 = "update MATERIA_PRIMA_ITEM set ESTADO='Reservado',ID_PEDIDO=" + id_pedido
                    + " where id=" + id;

            Statement st2 = con.createStatement();
            st2.executeUpdate(sql2);
            st2.close();
        }

        st.close();
        return i;
    }

    //-------------------------------------------------------
    // Métodos para ensayar otros métodos
    //-------------------------------------------------------
    @POST
    @Path("/reservarProducto")
    public void reservarProductoREST(List lista) throws Exception {

        abrirConexion();
        LinkedHashMap lNombreProducto = (LinkedHashMap) lista.get(0);
        LinkedHashMap lCantidad = (LinkedHashMap) lista.get(1);
        LinkedHashMap lIdPedido = (LinkedHashMap) lista.get(2);

        reservarProductoBodega(lNombreProducto.get("nombreProducto").toString(), (int) lCantidad.get("cantidad"),
                (int) lIdPedido.get("id_pedido"));

        cerrarConexion();
    }

    
    @POST
    @Path("/registrarProveedor")
    public void registrarProveedor(JSONObject proveedor) throws Exception {
        abrirConexion();
        System.out.println("- - - - - - - - - - - - - - - - - Print Entrada - - - - - - - - - - - - - - - - -");
        System.out.println(proveedor);
        int doc = Integer.parseInt(proveedor.get("documento").toString());
        String nombre = proveedor.get("nombre").toString();
        String ciudad = proveedor.get("ciudad").toString();
        String direccion = proveedor.get("direccion").toString();
        String telefono = proveedor.get("telefono").toString();
        int volumenMax = Integer.parseInt(proveedor.get("volumenMaximo").toString());
        int tiempoResp = Integer.parseInt(proveedor.get("tiempoDeEntrega").toString());
        String representante = proveedor.get("representanteLegal").toString();
//        String sql = "select max (id) as MAXIMO from PROVEEDOR";
//        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
//        System.out.println(sql);
//        Statement st = con.createStatement();
//        ResultSet rs = st.executeQuery(sql);
//        st.close();
//        int id_item = -1;
//        if (rs.next()) {
//            id_item = rs.getInt("MAXIMO") + 1;
//        }
        String sql = "INSERT INTO PROVEEDOR (DOCUMENTO_ID,NOMBRE,CIUDAD,DIRECCION,TELEFONO,VOLUMEN_MAXIMO,TIEMPO_DE_ENTREGA,REPRESENTANTE_LEGAL)" + " VALUES ('" + doc + "','" + nombre + "','" + ciudad + "','" + direccion + "','" + telefono + "'," + volumenMax + "," + tiempoResp + "," + representante + ")";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(sql);
        Statement st2 = con.createStatement();
        st2.executeUpdate(sql);
        st2.close();
        cerrarConexion();
    }

    @POST
    @Path("/registrarLlegadaDeMaterial")
    public void registrarLlegadaDeMaterial(JSONObject idPedidoMateriaPrimaP) throws Exception {
        abrirConexion();
        int idPedidoMateriaPrima = Integer.parseInt(idPedidoMateriaPrimaP.get("id").toString());
        System.out.println("Entrada parÃ¡metro registrarLlegadaDeMaterial");
        System.out.println(idPedidoMateriaPrima);
        Calendar c = new GregorianCalendar();
        String fecha = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                + (c.get(GregorianCalendar.MONTH)+1) + "-" + c.get(GregorianCalendar.YEAR);
        String query = "update PEDIDO_MATERIA_PRIMA set FECHA_ENTREGA = TO_DATE ('" + fecha + "','DD-MM-YYYY') WHERE ID = " + idPedidoMateriaPrima;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query);
        Statement st = con.createStatement();
        st.executeQuery(query);
        st.close();
        query = "update MATERIA_PRIMA_ITEM set ESTADO = 'En Bodega' WHERE ID_PEDIDO =" + idPedidoMateriaPrima;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query);
        st = con.createStatement();
        st.executeQuery(query);
        st.close();
        cerrarConexion();
    }

    @POST
    @Path("/registrarLlegadaDeComponentes")
    public void registrarLlegadaDeComponentes(JSONObject idPedidoComponenteP) throws Exception {
        abrirConexion();
        int idPedidoComponente = Integer.parseInt(idPedidoComponenteP.get("id").toString());
        System.out.println("Entrada parÃ¡metro registrarLlegadaDeMaterial");        
        System.out.println("Marca 1");
        System.out.println(idPedidoComponente);
        Calendar c = new GregorianCalendar();
        System.out.println("Marca 2");
        String fecha = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                + (c.get(GregorianCalendar.MONTH)+1) + "-" + c.get(GregorianCalendar.YEAR);
        System.out.println("Marca 3");
        String query = "update PEDIDO_COMPONENTE set FECHA_ENTREGA = TO_DATE ('" + fecha + "','DD-MM-YYYY') WHERE ID = " + idPedidoComponente;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query);
        Statement st = con.createStatement();
        st.executeQuery(query);
        st.close();
        query = "update COMPONENTE_ITEM set ESTADO = 'En Bodega' WHERE ID_PEDIDO =" + idPedidoComponente;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query);
        st = con.createStatement();
        st.executeQuery(query);
        st.close();
        cerrarConexion();
    }

    @POST
    @Path("/registrarEjecucionEtapa")
    public JSONObject registrarEjecucionEtapa(JSONObject num_secuenciaP) throws Exception {
        abrirConexion();
        int num_secuencia = Integer.parseInt(num_secuenciaP.get("secuencia").toString());
        //Verificar productos
        System.out.println("Entrada parÃ¡metro registrarEjecucionEtapa");
        System.out.println(num_secuencia);
        int numProductosDisponibles = verificarProductosEstacionAnterior(num_secuencia);
        if (numProductosDisponibles == 0) {
            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "Numero de productos no disponibles");
            return jRespuesta;
        }
        //Verificar componentes
        String query = "select * from (select CANTIDAD, (case when cuenta is null then 0 else cuenta end) as CUENTA from (select * from ITEM_COMPONENTE_ETAPA left outer join (select COMPONENTE, count(ID) as cuenta from COMPONENTE_ITEM where COMPONENTE_ITEM.ESTADO = 'En Bodega' group by COMPONENTE) on COMPONENTE = COMPONENTE_NOMBRE) where NUMERO_SECUENCIA ="+num_secuencia+") where CANTIDAD > cuenta";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (rs.next()) {

            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "Cantidad de componentes insuficientes");
            return jRespuesta;
        }
        st.close();
        //Verificar materia
        String query1 = "select * from (select CANTIDAD, (case when cuenta is null then 0 else cuenta end) as CUENTA from (select * from ITEM_MATERIA_PRIMA_ETAPA left outer join (select MATERIA, count(ID) as cuenta from MATERIA_PRIMA_ITEM where MATERIA_PRIMA_ITEM.ESTADO = 'En Bodega' group by MATERIA) on MATERIA = MATERIA_PRIMA_NOMBRE) where NUMERO_SECUENCIA ="+num_secuencia+") where CANTIDAD > cuenta";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query1);
        Statement st1 = con.createStatement();
        ResultSet rs1 = st1.executeQuery(query1);
        if (rs1.next()) {

            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "Cantidad de materia prima insuficiente");
            return jRespuesta;
        }
        st1.close();
        //Subir productos en etapas
        String query2 = "update item set ETAPA = ETAPA+1 where ETAPA = " + (num_secuencia-1) + "AND ID = (select min(ID) from ITEM where ETAPA = "+(num_secuencia-1)+")";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query2);
        Statement st2 = con.createStatement();
        st2.executeQuery(query2);
        st2.close();
        //Reducir suministros MATERIA PRIMA
        String query3 = "select * from ITEM_MATERIA_PRIMA_ETAPA where NUMERO_SECUENCIA = " + num_secuencia;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query3);
        Statement st3 = con.createStatement();
        ResultSet rs3 = st3.executeQuery(query3);
        ArrayList<String> lista1 = new ArrayList();
        ArrayList<Integer> lista2 = new ArrayList();
        while (rs3.next()) {
            lista1.add(rs3.getString("MATERIA_PRIMA_NOMBRE"));
            lista2.add(rs3.getInt("CANTIDAD"));
        }
        st3.close();
        String query4;
        Statement st4;
        ResultSet rs4;
        for (int i = 0; i < lista1.size(); i++) {
            for (int j = 0; j < lista2.get(i); j++) {
                query4 = "DELETE FROM MATERIA_PRIMA_ITEM where ID = (select min(ID) from MATERIA_PRIMA_ITEM where MATERIA = '" + lista1.get(i) + "')";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query4);
                st4 = con.createStatement();
                rs4 = st4.executeQuery(query4);
                st4.close();
            }
        }
        //Reducir suministros COMPONENTE
        query3 = "select * from ITEM_COMPONENTE_ETAPA where NUMERO_SECUENCIA = " + num_secuencia;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query3);
        st3 = con.createStatement();
        rs3 = st3.executeQuery(query3);
        lista1 = new ArrayList();
        lista2 = new ArrayList();
        while (rs3.next()) {
            lista1.add(rs3.getString("COMPONENTE_NOMBRE"));
            lista2.add(rs3.getInt("CANTIDAD"));
        }
        st3.close();
        for (int i = 0; i < lista1.size(); i++) {
            for (int j = 0; j < lista2.get(i); j++) {
                query4 = "DELETE FROM COMPONENTE_ITEM where ID = (select min(ID) from COMPONENTE_ITEM where COMPONENTE = '" + lista1.get(i) + "')";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query4);
                st4 = con.createStatement();
                rs4 = st4.executeQuery(query4);
                st4.close();
            }
        }
        cerrarConexion();

        JSONObject jRespuesta = new JSONObject();
        jRespuesta.put("Respuesta", "Operacion correcta");
        return jRespuesta;
    }

    @POST
    @Path("/verificarProductosEstacionAnterior")
    public int verificarProductosEstacionAnterior(int numSecuencia) throws Exception {
        //Dar etapa y producto del num_secuencia
//        String query3 = "select etapa, nombre_producto from ETAPA_DE_PRODUCCION where NUMERO_SECUENCIA = " + numSecuencia;
//        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
//        System.out.println(query3);
//        Statement st3 = con.createStatement();
//        ResultSet rs3 = st3.executeQuery(query3);
//        int resp3 = 0;
//        String resp4 = "";
//        if (rs3.next()) {
//            resp3 = rs3.getInt("etapa");
//            System.out.println("etapa:");
//            System.out.println(resp3);
//            resp4 = rs3.getString("nombre_producto");
//            System.out.println("nombre_producto:");
//            System.out.println(resp4);
//        }
//        int etapa = resp3;
//        int etapaAnterior = etapa - 1;
//        String producto = resp4;
//        st3.close();
//        //Dar id de la etapa anterior
//        String query4 = "select NUMERO_SECUENCIA from ETAPA_DE_PRODUCCION where ETAPA= " + etapaAnterior + " AND " + "NOMBRE_PRODUCTO ='" + producto + "'";
//        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
//        System.out.println(query4);
//        Statement st4 = con.createStatement();
//        ResultSet rs4 = st4.executeQuery(query4);
//        int resp5 = 0;
//        if (rs4.next()) {
//            resp5 = rs4.getInt("NUMERO_SECUENCIA");
//            System.out.println("NUMERO_SECUENCIA:");
//            System.out.println(resp5);
//        }
//        st4.close();
//        int numSecAnterior = resp5;
        //Dar numero de items en etapa anterior
        String query5 = "select count(*) as cuenta from ITEM where ETAPA= " + (numSecuencia-1);
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query5);
        Statement st5 = con.createStatement();
        ResultSet rs5 = st5.executeQuery(query5);
        int resp6 = 0;
        if (rs5.next()) {
            resp6 = rs5.getInt("cuenta");
            System.out.println("cuenta:");
            System.out.println(resp6);
        }
        st5.close();
        //FIN
        System.out.println("Return verificarProductosEstacionAnterior: " + resp6);
        return resp6;
    }
}

package ant.algorithm;

import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Animation.Status;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.GroupBuilder;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContextMenuBuilder;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuBarBuilder;
import javafx.scene.control.MenuBuilder;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MenuItemBuilder;
import javafx.scene.control.Slider;
import javafx.scene.control.SliderBuilder;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javax.swing.Timer;

public class AntAlgorithm extends Application {

    private Group group_grid, group_root, group_left;
    private VBox group_right, group_time;
    private HBox group_graf_buttons, group_sim_buttons;
    private Slider slider_animation_timer;
    private Text txt_edge_info, txt_animation_timer;
    private ContextMenu contex_menu_circle, contex_menu_edge;
    private MenuItem contex_menu_item_food2, contex_menu_item_food, contex_menu_item_cave, contex_menu_item_delete_circle, contex_menu_item_delete_edge;
    private ToggleButton button_draw_graf;
    private Button button_clear_graf, button_start_sim, button_stop_sim;
    private Circle circle_last, circle_context, circle_cave_ex;
    private TranslateTransition[] animations;
    private GrafLine line_context;
    private Timer my_timer;
    private Image_ant[] my_little_ants;
    private Circle[][] circle_grid;
    private int[][] all_neihgbours_mat;
    private ArrayList possibilities, ants_neighbours;
    private GrafLine[] edges;
    private Builders my_builder;
    private Scene my_scene;
    private Random my_random;
    private Operations Ops;
    private Stage my_stage;
    private int offset_x = 10, offset_y = 10, graf_space = 45, q_coef = 5000, ant_count = 100, right_width=450, grid_width, grid_height, screen_width, screen_height;
    private double evaporation = 12, animation_time, alpha = 1, beta = 2;
    private int animation_time_coef = 20;

    @Override
    public void start(Stage primaryStage) {
        my_stage = primaryStage;
        animation_time = (double) 1 / animation_time_coef;
        animations = new TranslateTransition[0];
        prepare_variables();
        group_root = GroupBuilder.create().layoutX(offset_x).layoutY(offset_y).build();
        group_right = prepare_group_right();
        group_left = prepare_group_left();
        my_timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Platform.runLater(new Task() {
                    @Override
                    protected Object call() throws Exception {
                        feromon_update();
                        return 1;
                    }
                });
            }
        });
        group_root.getChildren().addAll(group_left, group_right);
        my_scene = SceneBuilder.create().width(screen_width - 50).height(screen_height - 100).root(group_root).build();
        prepare_stage();
    }

    @Override
    public void stop() throws Exception {
        my_timer.stop();
    }

    private void feromon_update() {
        for (int m = 0; m < edges.length; m++) {
            edges[m].update_feromon();
        }
    }

    private void feromon_update(Image_ant image_ant) {
        ArrayList temp = image_ant.get_paths();
        int path_length = 0;
        for (int m = 0; m < temp.size(); m++) {
            for (int k = 0; k < edges.length; k++) {
                if (edges[k].getId().equals(temp.get(m).toString())) {
                    path_length += edges[k].get_length();
                    break;
                }
            }
        }
        for (int m = 0; m < temp.size(); m++) {
            for (int k = 0; k < edges.length; k++) {
                if (edges[k].getId().equals(temp.get(m).toString())) {
                    edges[k].update_feromon_2(q_coef, path_length);
                    break;
                }
            }
        }
    }

    private void create_animation(String ant_name) {
        int old_id, m = -1;
        for (int t = 0; t < my_little_ants.length; t++) {
            if (my_little_ants[t].get_name().equals(ant_name)) {
                m = t;
                break;
            }
        }
        old_id = Integer.parseInt(my_little_ants[m].getId());
        if (circle_grid[old_id / grid_width][old_id % grid_width].getFill() == Colors_and_shapes.color_food) {
            if (!my_little_ants[m].is_found_food()) {
                feromon_update(my_little_ants[m]);
                my_little_ants[m].clear_paths();
            }
            my_little_ants[m].set_found_food(true);
        }else if (circle_grid[old_id / grid_width][old_id % grid_width].getFill() == Colors_and_shapes.color_food2) {
            if (!my_little_ants[m].is_found_food()) {
                feromon_update(my_little_ants[m]);
                my_little_ants[m].clear_paths();
            }
            my_little_ants[m].set_found_food(true);
        } else if (circle_grid[old_id / grid_width][old_id % grid_width].getFill() == Colors_and_shapes.color_cave) {
            if (my_little_ants[m].is_found_food()) {
                feromon_update(my_little_ants[m]);
                my_little_ants[m].clear_paths();
            }
            my_little_ants[m].set_found_food(false);
        }
        ants_neighbours.clear();
        for (int j = 0; j < all_neihgbours_mat[old_id].length; j++) {
            if (all_neihgbours_mat[old_id][j] == 1) {
                ants_neighbours.add(j);
            }
        }
        Collections.shuffle(ants_neighbours);
        possibilities.clear();
        double total_value = 0, komsu_deger;
        for (int i = 0; i < ants_neighbours.size(); i++) {
            for (int k = 0; k < edges.length; k++) {
                if (edges[k].getId().equals(Ops.id_calc(old_id, Integer.parseInt(ants_neighbours.get(i).toString())))) {
                    total_value += Math.pow(edges[k].get_feromon(), alpha) * Math.pow(edges[k].get_length(), beta);
                    possibilities.add(Math.pow(edges[k].get_feromon(), alpha) * Math.pow(edges[k].get_length(), beta));
                    break;
                }
            }
        }
        int random_max = 0, temp;
        for (int i = 0; i < possibilities.size(); i++) {
            komsu_deger = Double.parseDouble(possibilities.get(i).toString());
            if (total_value == 0) {
                possibilities.set(i, 1);
                random_max++;
            } else {
                temp = (int) ((komsu_deger / total_value) * 1000) < 1 ? 1 : (int) ((komsu_deger / total_value) * 1000);
                possibilities.set(i, temp);
                random_max += temp;
            }
        }
        int path_to_go = -1, some_random;
        boolean is_possible = possibilities.size() > 1;
        boolean is_any_path_suitable = my_little_ants[m].is_any_path_suitable(ants_neighbours);
        if (!is_any_path_suitable) {
            //geri_sar();
        }
        do {
            do {
                some_random = my_random.nextInt(random_max);
                for (int k = 0; k < possibilities.size(); k++) {
                    some_random -= Double.parseDouble(possibilities.get(k).toString());
                    if (some_random < 0) {
                        path_to_go = Integer.parseInt(ants_neighbours.get(k).toString());
                        break;
                    }
                }
            } while (is_possible && path_to_go == Integer.parseInt(my_little_ants[m].get_last_id()));
        } while (!my_little_ants[m].path_is_available(path_to_go) && is_any_path_suitable && is_possible);
        my_little_ants[m].setId(path_to_go + "");
        my_little_ants[m].set_last_id(old_id + "");
        my_little_ants[m].add_path(Ops.id_calc(old_id, path_to_go));
        int new_pos_x = offset_x + (graf_space * (path_to_go % grid_width)) - 10;
        int new_pos_y = offset_y + (graf_space * (path_to_go / grid_width)) - 10;
        GrafLine temp_line = edges[0];
        for (int k = 0; k < edges.length; k++) {
            if (edges[k].getId().equals(Ops.id_calc(old_id, path_to_go))) {
                temp_line = edges[k];
                break;
            }
        }
        int anim = -1;
        for (int k = 0; k < animations.length; k++) {
            if (((Image_ant) animations[k].getNode()).get_name().equals(my_little_ants[m].get_name())) {
                anim = k;
                //System.out.println(animations[k].getNode());
                break;
            }
        }
        animations[anim] = my_builder.build_translate_transition(my_little_ants[m], animation_time, new_pos_x, new_pos_y, temp_line);
        int old_x = (int) my_little_ants[m].getTranslateX(), old_y = (int) my_little_ants[m].getTranslateY();
        my_little_ants[m].setRotate(Ops.calc_rotate(old_x, old_y, new_pos_x, new_pos_y));
        final String recall_name = my_little_ants[m].get_name();
        animations[anim].setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                create_animation(recall_name);
            }
        });
        animations[anim].playFromStart();
    }

    private void reset_neighbours() {
        all_neihgbours_mat = new int[grid_width * grid_height][grid_width * grid_height];
        for (int m = 0; m < all_neihgbours_mat.length; m++) {
            all_neihgbours_mat[m][m] = 0;
        }
    }

    private void prepare_grid() {
        grid_width = (int) (screen_width - offset_x - right_width - graf_space) / graf_space;
        grid_height = (int) (screen_height - (2 * graf_space)) / graf_space;
        circle_grid = new Circle[grid_height][grid_width];
        for (int m = 0; m < grid_height; m++) {
            for (int k = 0; k < grid_width; k++) {
                Circle temp_circle = my_builder.build_circle_grid(k + (grid_width * m), offset_x + (graf_space * k), offset_y + (graf_space * m));
                temp_circle.setOnMouseClicked(graf_mouse_clicked());
                group_grid.getChildren().add(temp_circle);
                circle_grid[m][k] = temp_circle;
            }
        }
        reset_neighbours();
    }

    private void delete_neighbour(int n_1, int n_2) {
        all_neihgbours_mat[n_1][n_2] = 0;
        all_neihgbours_mat[n_2][n_1] = 0;
    }

    private void delete_neighbour(int n_1) {
        for (int m = 0; m < edges.length; m++) {
            String[] id = edges[m].getId().split(",");
            if (Integer.parseInt(id[0]) == n_1 || Integer.parseInt(id[1]) == n_1) {
                edges[m].setVisible(false);
            }
        }
        for (int m = 0; m < all_neihgbours_mat.length; m++) {
            all_neihgbours_mat[n_1][m] = 0;
            all_neihgbours_mat[m][n_1] = 0;
        }
    }

    //inicializa entorno
    private void prepare_variables() {
        edges = new GrafLine[0];
        my_random = new Random();
        Ops = new Operations();
        my_builder = new Builders();
        possibilities = new ArrayList();
        ants_neighbours = new ArrayList();
        screen_width = (int) Screen.getPrimary().getBounds().getWidth();
        screen_height = (int) Screen.getPrimary().getBounds().getHeight();
    }

    //abrir programa
    private void prepare_stage() {
        my_stage.setTitle("Simulación colonia de hormigas");
        my_stage.setScene(my_scene);
        my_stage.setWidth(screen_width - 50);
        my_stage.setHeight(screen_height - 100);
        my_stage.getIcons().add((new Image(AntAlgorithm.class.getResourceAsStream("imgs/ant.png"))));
        my_stage.show();
    }

    //velocidad
    private VBox prepare_group_time() {
        slider_animation_timer = SliderBuilder.create().blockIncrement(1).showTickMarks(true).majorTickUnit(1).value(animation_time * animation_time_coef).min(0).max(100).build();
        slider_animation_timer.valueProperty().addListener(animation_timer_changed());
        txt_animation_timer = my_builder.build_text("Velocidad animación = " + (int) slider_animation_timer.getValue());
        return VBoxBuilder.create().children(txt_animation_timer, slider_animation_timer).spacing(5).build();
    }

    //nodos y rutas
    private Group prepare_group_left() {
        group_grid = GroupBuilder.create().build();
        prepare_grid();
        group_grid.setLayoutX(0);
        group_grid.setLayoutY(45);
        return GroupBuilder.create().children(group_grid).build();
    }

    //botones y opciones
    private VBox prepare_group_right() {
        group_time = prepare_group_time();
        group_graf_buttons = prepare_group_graf_buttons();
        group_sim_buttons = prepare_group_sim_buttons();
        contex_menu_circle = prepare_contex_menu_circle();
        contex_menu_edge = prepare_contex_menu_edge();
        txt_edge_info = my_builder.build_text("");
        return VBoxBuilder.create().layoutX(screen_width - right_width - 40).spacing(5).prefWidth(right_width).children(group_time, group_graf_buttons, group_sim_buttons, txt_edge_info).build();
    }

    //botones dibujar y limpiar
    private HBox prepare_group_graf_buttons() {
        button_draw_graf = ToggleButtonBuilder.create().prefWidth(200).prefHeight(30).text("Dibujar Rutas").build();
        button_draw_graf.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (((ToggleButton) event.getSource()).isSelected()) {
                    ((ToggleButton) event.getSource()).setText("Dibujando");
                    button_start_sim.setDisable(true);
                    button_clear_graf.setDisable(true);
                } else {
                    ((ToggleButton) event.getSource()).setText("Dibujar Rutas");
                    button_start_sim.setDisable(false);
                    button_clear_graf.setDisable(false);
                }
                circle_last = null;
            }
        });
        button_clear_graf = ButtonBuilder.create().prefWidth(200).prefHeight(30).text("Limpiar").build();
        button_clear_graf.setOnAction(clear_graf());
        return HBoxBuilder.create().children(button_draw_graf, button_clear_graf).spacing(5).build();
    }

    //botones empezar y detener
    private HBox prepare_group_sim_buttons() {
        button_start_sim = ButtonBuilder.create().prefWidth(200).prefHeight(30).text("Empezar").build();
        button_start_sim.setOnAction(start_sim());
        button_stop_sim = ButtonBuilder.create().prefWidth(200).disable(true).prefHeight(30).text("Detener").onAction(stop_sim()).build();
        return HBoxBuilder.create().children(button_start_sim, button_stop_sim).spacing(5).build();
    }

    //opciones de camino
    private ContextMenu prepare_contex_menu_edge() {
        contex_menu_item_delete_edge = MenuItemBuilder.create().text("Borrar camino").onAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                line_context.setVisible(false);
                delete_neighbour(line_context.get_neighbour_1(), line_context.get_neighbour_2());
            }
        }).build();
        return ContextMenuBuilder.create().items(contex_menu_item_delete_edge).build();
    }

    //opciones de nodos
    private ContextMenu prepare_contex_menu_circle() {
        contex_menu_item_food = MenuItemBuilder.create().text("Zona de riesgo Tipo1").onAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for (int m = 0; m < circle_grid.length; m++) {
                    for (int k = 0; k < circle_grid[m].length; k++) {
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_food) {
//                            circle_grid[m][k].setFill( Colors_and_shapes.color_graf );
//                            circle_grid[m][k].setRadius( Colors_and_shapes.radius_grid );
                        }
                    }
                }
                circle_context.setFill(Colors_and_shapes.color_food);
                circle_context.setRadius(Colors_and_shapes.radius_food);
            }
        }).build();
        contex_menu_item_food2 = MenuItemBuilder.create().text("Zona de riesgo Tipo2").onAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for (int m = 0; m < circle_grid.length; m++) {
                    for (int k = 0; k < circle_grid[m].length; k++) {
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_food2) {
//                            circle_grid[m][k].setFill( Colors_and_shapes.color_graf );
//                            circle_grid[m][k].setRadius( Colors_and_shapes.radius_grid );
                        }
                    }
                }
                circle_context.setFill(Colors_and_shapes.color_food2);
                circle_context.setRadius(Colors_and_shapes.radius_food2);
            }
        }).build();
        contex_menu_item_cave = MenuItemBuilder.create().text("Almacén regional").onAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for (int m = 0; m < circle_grid.length; m++) {
                    for (int k = 0; k < circle_grid[m].length; k++) {
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_cave) {
                            circle_grid[m][k].setFill(Colors_and_shapes.color_graf);
                            circle_grid[m][k].setRadius(Colors_and_shapes.radius_grid);
                        }
                    }
                }
                circle_context.setFill(Colors_and_shapes.color_cave);
                circle_context.setRadius(Colors_and_shapes.radius_cave);
            }
        }).build();
        contex_menu_item_delete_circle = MenuItemBuilder.create().text("Borrar nodo").onAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                circle_context.setFill(Colors_and_shapes.color_grid);
                circle_context.setRadius(Colors_and_shapes.radius_grid);
                delete_neighbour(Integer.parseInt(circle_context.getId()));
            }
        }).build();
        return ContextMenuBuilder.create().items(contex_menu_item_delete_circle,contex_menu_item_food2, contex_menu_item_food, contex_menu_item_cave).build();
    }

    //evento click para dibujar
    private EventHandler<MouseEvent> graf_mouse_clicked() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.SECONDARY) {
                    if (!button_start_sim.isDisabled() && ((Circle) event.getSource()).getFill() == Colors_and_shapes.color_graf) {
                        circle_context = (Circle) event.getSource();
                        contex_menu_circle.show(circle_context, circle_context.getCenterX(), circle_context.getCenterY());
                        circle_last = null;
                    }
                } else if (button_draw_graf.isSelected()) {
                    Circle circle_temp = ((Circle) event.getSource());
                    circle_temp.setFill(Colors_and_shapes.color_graf);
                    circle_temp.setRadius(Colors_and_shapes.radius_grid);
                    if (circle_last != null && !circle_last.equals(circle_temp)) {
                        String id = Ops.id_calc(Integer.parseInt(circle_last.getId()), Integer.parseInt(circle_temp.getId()));
                        GrafLine new_line = my_builder.build_line(id, (int) circle_last.getCenterX(), (int) circle_last.getCenterY(), (int) circle_temp.getCenterX(), (int) circle_temp.getCenterY(), evaporation, Integer.parseInt(circle_last.getId()), Integer.parseInt(circle_temp.getId()));
                        new_line.setOnMouseClicked(line_mouse_clicked());
                        group_grid.getChildren().add(new_line);
                        group_grid.getChildren().add(new_line.text_weight());
                        new_line.toBack();
                        edges = Ops.add_edge(edges, new_line);
                        all_neihgbours_mat[Integer.parseInt(circle_temp.getId())][Integer.parseInt(circle_last.getId())] = 1;
                        all_neihgbours_mat[Integer.parseInt(circle_last.getId())][Integer.parseInt(circle_temp.getId())] = 1;
                        circle_last.setFill(Colors_and_shapes.color_graf);
                        circle_last = null;
                    } else {
                        circle_last = circle_temp;
                        circle_last.setFill(Colors_and_shapes.color_last);
                    }
                }
            }
        };
    }

    //crear linea dibujo
    private EventHandler<MouseEvent> line_mouse_clicked() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.SECONDARY) {
                    line_context = (GrafLine) event.getSource();
                    contex_menu_edge.show(line_context, event.getScreenX(), event.getScreenY());
                }
            }
        };
    }

    //detiene hormigas
    private EventHandler<ActionEvent> stop_sim() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                my_timer.stop();
                for (int m = 0; m < animations.length; m++) {
                    animations[m].stop();
                }
                ((Button) event.getSource()).setDisable(true);
                button_start_sim.setDisable(false);
                for (int m = 0; m < grid_height; m++) {
                    for (int k = 0; k < grid_width; k++) {
                        circle_grid[m][k].setVisible(true);
                    }
                }
                for (int m = 0; m < my_little_ants.length; m++) {
                    my_little_ants[m].setVisible(false);
                    my_little_ants[m] = null;
                }
                for (int m = 0; m < edges.length; m++) {
                    edges[m].set_feromon_zero();
                }
                my_little_ants = new Image_ant[0];
                button_clear_graf.setDisable(false);
                button_draw_graf.setDisable(false);
            }
        };
    }

    //inicia hormigas
    private EventHandler<ActionEvent> start_sim() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean cave_exists = false, food_sxists = false, graf_exists = false, graf_complete = true;
                for (int m = 0; m < grid_height; m++) {
                    for (int k = 0; k < grid_width; k++) {
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_cave) {
                            cave_exists = true;
                        }
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_food) {
                            food_sxists = true;
                        }
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_food2) {
                            food_sxists = true;
                        }
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_graf) {
                            graf_exists = true;
                        }
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_last) {
                            graf_complete = false;
                        }
                    }
                }
                if (!graf_exists) {
                    Message_box.show("Primero dibuje la ruta", "Aviso", Message_box.warning_message);
                    return;
                }
                if (!graf_complete) {
                    Message_box.show("Complete el camino, no deje nodos sin unir", "Aviso", Message_box.warning_message);
                    return;
                }
                if (!cave_exists) {
                    Message_box.show("Agregue almacén regional", "Aviso", Message_box.warning_message);
                    return;
                }
                if (!food_sxists) {
                    Message_box.show("Agregue zona de riesgo", "Aviso", Message_box.warning_message);
                    return;
                }
                button_clear_graf.setDisable(true);
                button_draw_graf.setDisable(true);
                my_little_ants = new Image_ant[0];
                ((Button) event.getSource()).setDisable(true);
                button_stop_sim.setDisable(false);
                for (int m = 0; m < edges.length; m++) {
                    edges[m].set_feromon_zero();
                }
                //clear graf
                for (int m = 0; m < grid_height; m++) {
                    for (int k = 0; k < grid_width; k++) {
                        if (circle_grid[m][k].getFill().equals(Colors_and_shapes.color_grid)) {
                            circle_grid[m][k].setVisible(false);
                        }
                    }
                }
                //create ants
                animations = new TranslateTransition[ant_count];
                boolean cave = false;
                Circle cave_circle = circle_cave_ex;
                for (int m = 0; m < grid_height; m++) {
                    for (int k = 0; k < grid_width; k++) {
                        if (circle_grid[m][k].getFill() == Colors_and_shapes.color_cave) {
                            cave = true;
                            cave_circle = circle_grid[m][k];
                            break;
                        }
                    }

                    if (cave) {
                        break;
                    }
                }
                Image_ant ant;
                try {
                    PrintStream out = new PrintStream(new FileOutputStream("./OutFile.txt"));
                    for (int m = 0; m < ant_count; m++) {
                        ant = my_builder.build_image_ant(Integer.parseInt(cave_circle.getId()), (int) cave_circle.getCenterX(), (int) cave_circle.getCenterY(), "k" + m);
                        group_grid.getChildren().add(ant);
                        my_little_ants = Ops.add_circle(my_little_ants, ant);
                        animations[m] = new TranslateTransition();
                        animations[m].setNode(ant);
                        //System.out.println(my_little_ants[m].get_name());
                        create_animation(my_little_ants[m].get_name());
                        out.println(my_little_ants[m].get_paths());
                    }
                    out.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(AntAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
                }

                my_timer.restart();
            }
        };
    }

    //limpia nodos
    private EventHandler<ActionEvent> clear_graf() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for (int m = 0; m < grid_height; m++) {
                    for (int k = 0; k < grid_width; k++) {
                        circle_grid[m][k].setFill(Colors_and_shapes.color_grid);
                        circle_grid[m][k].setRadius(Colors_and_shapes.radius_grid);
                    }
                }
                reset_neighbours();
                for (int m = 0; m < edges.length; m++) {
                    edges[m].setVisible(false);
                    edges[m] = null;
                }
                edges = new GrafLine[0];
                circle_last = null;
            }
        };
    }

    //evento que cambia velocidad
    private ChangeListener<Number> animation_timer_changed() {
        return new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                txt_animation_timer.setText("Velocidad animación = " + newValue.intValue());
                animation_time = (double) newValue.intValue() / animation_time_coef;
                my_timer.setDelay(animation_time != 0 ? (int) (500 / animation_time) : Integer.MAX_VALUE);
                my_timer.restart();
                if (newValue.intValue() == 0) {
                    for (int m = 0; m < animations.length; m++) {
                        animations[m].pause();
                    }
                } else {
                    for (int m = 0; m < animations.length; m++) {
                        if (animations[m].statusProperty().get() == Status.PAUSED) {
                            animations[m].play();
                        }
                    }
                }
            }
        };
    }
}
